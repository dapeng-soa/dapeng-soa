package com.github.dapeng.json;


import com.github.dapeng.core.*;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.dapeng.core.enums.CodecProtocol.CompressedBinary;
import static com.github.dapeng.json.JsonUtils.*;
import static com.github.dapeng.util.MetaDataUtil.dataType2Byte;
import static com.github.dapeng.util.MetaDataUtil.findEnumItemValue;

/**
 * format:
 * url:http://xxx/api/callService?serviceName=xxx&version=xx&method=xx
 * post body:
 * <pre>
 * {
 *  "body":{
 *      ${structName}:${struct}
 *  }
 * }
 * </pre>
 */

class JsonReader implements JsonCallback {
    private final Logger logger = LoggerFactory.getLogger(JsonReader.class);

    private final OptimizedMetadata.OptimizedStruct optimizedStruct;
    private final OptimizedMetadata.OptimizedService optimizedService;
    private final ByteBuf requestByteBuf;

    private final InvocationContext invocationCtx = InvocationContextImpl.Factory.currentInstance();


    /**
     * 压缩二进制编码下,集合的默认长度(占3字节)
     */
    private final TProtocol oproto;

    TJsonCompressProtocolCodec jsonCompressProtocolCodec = new TJsonCompressProtocolCodec();

    /**
     * 当前处理数据节点
     */
    StackNode current;

    /**
     * incr: startObject/startArray
     * decr: endObject/endArray
     */
    int level = -1;

    /**
     * onStartField的时候, 记录是否找到该Field. 如果没找到,那么需要skip这个field
     * 当 skip 设置为 true 时， skipDepth 初始化为 0。
     * - onStartObject/onStartArray) 则 skipDepth++
     * - onEndObject/onEndArray时，skipDepth--
     * 当 skipDepth 为 0 且进入 onEndField 时， skip 复位为false
     */
    boolean skip = false;
    int skipDepth = 0;

    /**
     * <pre>
     * - onStartObject
     *  - 当前栈顶 ：STRUCT or MAP
     *  - 栈操作：无
     *  - 处理：proto.writeStructBegin or proto.writeMapBegin
     * - onEndObject
     *  - 当前栈顶：STRUCT or MAP
     *  - 栈操作：无
     *  - 处理：
     *      - proto.writeStructEnd or proto.writeMapEnd
     *      - 如果父栈元素是 LIST/SET/MAP，计数++
     * - onStartArray
     *  - 当前栈顶：LIST/SET
     *  - 栈操作：无
     *  - 处理：proto.writeListBegin or proto.writeSetBegin
     * - onEndArray
     *  - 当前栈顶：
     *  - 栈操作：无
     *  - 处理：
     *      - proto.writeListEnd or proto.writeSetEnd
     *      - 如果父栈元素是 LIST/SET/MAP，计数++
     * - onStartField name
     *  - 当前栈顶：STRUCT / MAP
     *  - 栈操作：
     *      - STRUCT：将结构体的fields[name] 压栈
     *      - MAP：将 valueType 压栈
     *      - 同步新栈顶的 writeIndex
     *  - 处理：
     *      - STRUCT: proto.writeFieldBegin name
     *      - MAP: proto.writeString name or proto.writeInt name.toInt
     * - onEndField
     *  - 当前栈顶：any
     *  - 栈操作：pop 恢复当前 STRUCT/MAP
     *  - 处理：proto.writeFieldEnd
     * - onNumber
     *  - 当前栈顶：BYTE/SHORT/INTEGER/LONG/DOUBLE
     *  - 栈操作：无
     *  - 处理
     *      - proto.writeI8, writeI16, ...
     *      - 如果父栈元素是 LIST/SET/MAP，计数++
     * - onBoolean
     *  - 当前栈顶：BOOLEAN
     *  - 栈操作：无
     *  - 处理
     *      - proto.writeBoolean.
     *      - 如果父栈元素是 LIST/SET/MAP，计数++
     * - onString
     *  - 当前栈顶：STRING
     *  - 栈操作：无
     *  - 处理
     *      - proto.writeString, ...
     *      - 如果父栈元素是 LIST/SET/MAP，计数++
     * - onNull
     *  - 当前栈顶：any
     *  - 栈操作：无
     *  - 处理
     *      - 父栈元素为 STRUCT/MAP 时，当前field容许为空时，重置 writeIndex,否则报错
     *      - 父栈元素为 LIST/SET 时，不容许写入空值。
     * </pre>
     */
    Stack<StackNode> history = new Stack<>();

    List<StackNode> nodePool = new ArrayList<>(64);  // keep a minum StackNode Pool

    /**
     * @param optimizedStruct
     * @param optimizedService
     * @param requestByteBuf
     * @param oproto
     */
    JsonReader(OptimizedMetadata.OptimizedStruct optimizedStruct, OptimizedMetadata.OptimizedService optimizedService, ByteBuf requestByteBuf, TProtocol oproto) {
        this.optimizedStruct = optimizedStruct;
        this.optimizedService = optimizedService;
        this.requestByteBuf = requestByteBuf;
        this.oproto = oproto;
    }


    @Override
    public void onStartObject() throws TException {
        level++;

        if (level == 0) return;  // it's the outside { body: ... } object

        if (skip) {
            skipDepth++;
            return;
        }

        assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.incrElementSize();
        }
        switch (current.dataType.kind) {
            case STRUCT:
                Struct struct = current.optimizedStruct.struct;
                if (struct == null) {
                    logger.error("optimizedStruct not found");
                    logAndThrowTException();
                }
                oproto.writeStructBegin(new TStruct(struct.name));
                break;
            case MAP:
                assert isValidMapKeyType(current.dataType.keyType.kind);
                writeMapBegin(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), 0);
                break;
            default:
                logAndThrowTException();
        }

    }

    @Override
    public void onEndObject() throws TException {
        level--;
        if (level == -1) return; // the outer body

        if (skip) {
            skipDepth--;
            return;
        }

        assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

        switch (current.dataType.kind) {
            case STRUCT:
                validateStruct(current);
                oproto.writeFieldStop();
                oproto.writeStructEnd();
                break;
            case MAP:
                oproto.writeMapEnd();
                reWriteByteBuf();
                break;
            default:
                logAndThrowTException();
        }

    }

    /**
     * 由于目前拿不到集合的元素个数, 暂时设置为0个
     *
     * @throws TException
     */
    @Override
    public void onStartArray() throws TException {
        level++;
        if (skip) {
            skipDepth++;
            return;
        }

        assert isCollectionKind(current.dataType.kind);

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.incrElementSize();
        }

        switch (current.dataType.kind) {
            case LIST:
            case SET:
                writeCollectionBegin(dataType2Byte(current.dataType.valueType), 0);
                break;
            default:
                logAndThrowTException();
        }

    }

    @Override
    public void onEndArray() throws TException {
        level--;
        if (skip) {
            skipDepth--;
            return;
        }

        assert isCollectionKind(current.dataType.kind);

        switch (current.dataType.kind) {
            case LIST:
                oproto.writeListEnd();
                reWriteByteBuf();
                break;
            case SET:
                oproto.writeSetEnd();
                reWriteByteBuf();
                break;
            default:
                //do nothing
        }
    }

    @Override
    public void onStartField(String name) throws TException {
        if (skip) {
            return;
        }
        if (level == 0) { // expect only the "body"
            if ("body".equals(name)) { // body
                DataType initDataType = new DataType();
                initDataType.setKind(DataType.KIND.STRUCT);
                initDataType.qualifiedName = optimizedStruct.struct.name;
                push(initDataType, -1, // not a struct field
                        requestByteBuf.writerIndex(), //
                        optimizedStruct, "body");
            } else { // others, just skip now
                skip = true;
                skipDepth = 0;
            }
        } else { // level > 0, not skip
            if (current.dataType.kind == DataType.KIND.MAP) {
                assert isValidMapKeyType(current.dataType.keyType.kind);

                int tFieldPos = requestByteBuf.writerIndex();
                if (current.dataType.keyType.kind == DataType.KIND.STRING) {
                    oproto.writeString(name);
                } else {
                    writeIntField(name, current.dataType.keyType.kind);
                }
                push(current.dataType.valueType,
                        tFieldPos, // so value can't be null
                        requestByteBuf.writerIndex(), // need for List/Map
                        optimizedService.optimizedStructs.get(current.dataType.valueType.qualifiedName),
                        name);
            } else if (current.dataType.kind == DataType.KIND.STRUCT) {

                Field field = current.optimizedStruct.fieldMap.get(name);
                if (field == null) {
                    skip = true;
                    skipDepth = 0;
                    logger.debug("field(" + name + ") not found. just skip");
                    return;
                } else {
                    skip = false;
                }

                int tFieldPos = requestByteBuf.writerIndex();
                oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
                push(field.dataType,
                        tFieldPos,
                        requestByteBuf.writerIndex(),
                        optimizedService.optimizedStructs.get(field.dataType.qualifiedName),
                        name);
            } else
                logAndThrowTException("field " + name + " type " + toString(current.dataType) + " not compatible with json object");
        }
    }

    @Override
    public void onStartField(int index) {
        if (skip) {
            return;
        }
        assert isCollectionKind(current.dataType.kind);

        DataType next = current.dataType.valueType;
        OptimizedMetadata.OptimizedStruct nextStruct = (next.kind == DataType.KIND.STRUCT) ?
                optimizedService.optimizedStructs.get(next.qualifiedName) : null;
        push(current.dataType.valueType,
                -1,
                requestByteBuf.writerIndex(),
                nextStruct,
                null);

    }

    @Override
    public void onEndField() throws TException {
        if (skip) {
            if (skipDepth == 0) { // reset skipFlag
                skip = false;
            }
            return;
        }

        String fieldName = current.fieldName;

        if (level > 0) { // level = 0 will having no current dataType
            StackNode parent = peek();
            assert (parent != null);

            switch (parent.dataType.kind) {
                case SET:
                case LIST:
                    if (current.isNull) {
                        logAndThrowTException("SET/LIST can't support null value");
                    }
                    break;
                case MAP:
                    if (current.isNull) {
                        // peek().decrElementSize(); onNull not incrElementSize
                        requestByteBuf.writerIndex(current.tFieldPosition);
                    }
                    break;
                case STRUCT:
                    if (current.isNull) {
                        // parent.fields4Struct.remove(fieldName);
                        requestByteBuf.writerIndex(current.tFieldPosition);
                        if (invocationCtx.codecProtocol() == CompressedBinary) {
                            ((TCompactProtocol) oproto).resetLastFieldId();
                        }
                    } else {
                        Field field = parent.optimizedStruct.fieldMap.get(fieldName);
                        parent.fields4Struct.set(field.tag - parent.optimizedStruct.tagBase);
                        oproto.writeFieldEnd();
                    }
                    break;
            }

            pop();
        }

    }

    @Override
    public void onBoolean(boolean value) throws TException {
        if (skip) {
            return;
        }

        if (current.dataType.kind != DataType.KIND.BOOLEAN) {
            logAndThrowTException();
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.incrElementSize();
        }

        oproto.writeBool(value);
    }

    @Override
    public void onNumber(double value) throws TException {
        DataType.KIND currentType = current.dataType.kind;

        if (skip) {
            return;
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.incrElementSize();
        }

        switch (currentType) {
            case SHORT:
                oproto.writeI16((short) value);
                break;
            case INTEGER:
            case ENUM:
                oproto.writeI32((int) value);
                break;
            case LONG:
                oproto.writeI64((long) value);
                break;
            case DOUBLE:
                oproto.writeDouble(value);
                break;
            case BIGDECIMAL:
                oproto.writeString(String.valueOf(value));
                break;
            case BYTE:
                oproto.writeByte((byte) value);
                break;
            default:
                throw new TException("Field:" + current.fieldName + ", DataType(" + current.dataType.kind
                        + ") for " + current.dataType.qualifiedName + " is not a Number");

        }
    }

    @Override
    public void onNumber(long value) throws TException {
        DataType.KIND currentType = current.dataType.kind;

        if (skip) {
            return;
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.incrElementSize();
        }

        switch (currentType) {
            case SHORT:
                oproto.writeI16((short) value);
                break;
            case INTEGER:
            case ENUM:
                oproto.writeI32((int) value);
                break;
            case LONG:
                oproto.writeI64((long) value);
                break;
            case DOUBLE:
                oproto.writeDouble(value);
                break;
            case BYTE:
                oproto.writeByte((byte) value);
                break;
            default:
                throw new TException("Field:" + current.fieldName + ", DataType(" + current.dataType.kind
                        + ") for " + current.dataType.qualifiedName + " is not a Number");

        }
    }

    @Override
    public void onNull() throws TException {
        if (skip) {
            return;
        }
        current.isNull = true;
    }

    @Override
    public void onString(String value) throws TException {
        if (skip) {
            return;
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.incrElementSize();
        }

        switch (current.dataType.kind) {
            case ENUM:
                TEnum tEnum = optimizedService.enumMap.get(current.dataType.qualifiedName);
                Integer tValue = findEnumItemValue(tEnum, value);
                if (tValue == null) logAndThrowTException();
                oproto.writeI32(tValue);
                break;
            case BOOLEAN:
                oproto.writeBool(Boolean.parseBoolean(value));
                break;
            case DOUBLE:
                oproto.writeDouble(Double.parseDouble(value));
                break;
            case BIGDECIMAL:
                oproto.writeString(value);
                break;
            case INTEGER:
                oproto.writeI32(Integer.parseInt(value));
                break;
            case LONG:
                oproto.writeI64(Long.parseLong(value));
                break;
            case SHORT:
                oproto.writeI16(Short.parseShort(value));
                break;
            default:
                if (current.dataType.kind != DataType.KIND.STRING) {
                    throw new TException("Field:" + current.fieldName + ", Not a real String!");
                }
                oproto.writeString(value);
        }
    }

    // only used in startField
    private void push(final DataType dataType, final int tFieldPos, final int valuePos, final OptimizedMetadata.OptimizedStruct optimizedStruct, String fieldName) {
        StackNode node = null;

        if (nodePool.size() > 0) node = nodePool.remove(nodePool.size() - 1);
        else node = new StackNode();

        node.init(dataType, valuePos, tFieldPos, optimizedStruct, fieldName);
        //if(current != null)
        history.push(node);
        this.current = node;
    }

    // only used in endField
    private StackNode pop() {
        StackNode old = history.pop();
        nodePool.add(old);

        return this.current = (history.size() > 0) ? history.peek() : null;
    }

    private StackNode peek() {
        return history.size() <= 1 ? null : history.get(history.size() - 2);
    }

    private String toString(DataType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.kind.toString());
        switch (type.kind) {
            case STRUCT:
                sb.append("(").append(type.qualifiedName).append(")");
                break;
            case LIST:
            case SET:
                sb.append("[").append(toString(type.valueType)).append("]");
                break;
            case MAP:
                sb.append("[").append(toString(type.keyType)).append(",").append(toString(type.valueType)).append("]");
                break;
        }
        return sb.toString();
    }

    private void validateStruct(StackNode current) throws TException {
        /**
         * 不在该Struct必填字段列表的字段列表
         */
        OptimizedMetadata.OptimizedStruct struct = current.optimizedStruct;
        for (Field field : struct.fieldMap.values()) {
            if (field != null && !field.isOptional() && !current.fields4Struct.get(field.tag - struct.tagBase)) {
                String fieldName = current.fieldName;
                String structName = struct.struct.name;
                SoaException ex = new SoaException(SoaCode.ReqFieldNull.getCode(), "JsonError, please check:"
                        + structName + "." + fieldName
                        + ", optimizedStruct mandatory fields missing:"
                        + field.name);
                logger.error(ex.getMessage());
                throw ex;
            }
        }
    }

    private void writeIntField(String value, DataType.KIND kind) throws TException {
        switch (kind) {
            case SHORT:
                oproto.writeI16(Short.parseShort(value));
                break;
            case INTEGER:
                oproto.writeI32(Integer.parseInt(value));
                break;
            case LONG:
                oproto.writeI64(Long.parseLong(value));
                break;
            default:
                logAndThrowTException();
        }
    }

    /**
     * 根据current 节点重写集合元素长度
     */
    private void reWriteByteBuf() throws TException {
        assert isMultiElementKind(current.dataType.kind);

        //拿到当前node的开始位置以及集合元素大小
        int beginPosition = current.valuePosition;
        int elCount = current.elCount;

        //备份最新的writerIndex
        int currentIndex = requestByteBuf.writerIndex();

        requestByteBuf.writerIndex(beginPosition);

        switch (current.dataType.kind) {
            case MAP:
                reWriteMapBegin(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), elCount);
                break;
            case SET:
            case LIST:
                reWriteCollectionBegin(dataType2Byte(current.dataType.valueType), elCount);
                break;
            default:
                logger.error("Field:" + current.fieldName + ", won't be here", new Throwable());
        }

        if (current.dataType.kind == DataType.KIND.MAP
                && invocationCtx.codecProtocol() == CompressedBinary
                && elCount == 0) {
            requestByteBuf.writerIndex(beginPosition + 1);
        } else {
            requestByteBuf.writerIndex(currentIndex);
        }
    }

    private void writeMapBegin(byte keyType, byte valueType, int defaultSize) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeMapBegin(new TMap(keyType, valueType, defaultSize));
                break;
            case CompressedBinary:
            default:
                jsonCompressProtocolCodec.writeMapBegin(keyType, valueType, requestByteBuf);
                break;
        }
    }

    private void reWriteMapBegin(byte keyType, byte valueType, int size) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeMapBegin(new TMap(keyType, valueType, size));
                break;
            case CompressedBinary:
            default:
                jsonCompressProtocolCodec.reWriteMapBegin(size, requestByteBuf);
                break;
        }
    }

    /**
     * TList just the same as TSet
     *
     * @param valueType
     * @param defaultSize
     * @throws TException
     */
    private void writeCollectionBegin(byte valueType, int defaultSize) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeListBegin(new TList(valueType, defaultSize));
                break;
            case CompressedBinary:
            default:
                jsonCompressProtocolCodec.writeCollectionBegin(valueType, requestByteBuf);
                break;
        }
    }

    private void reWriteCollectionBegin(byte valueType, int size) throws TException {
        switch (invocationCtx.codecProtocol()) {
            case Binary:
                oproto.writeListBegin(new TList(valueType, size));
                break;
            case CompressedBinary:
            default:
                jsonCompressProtocolCodec.reWriteCollectionBegin(size, requestByteBuf);
                break;
        }
    }


    private void logAndThrowTException() throws TException {
        String fieldName = current == null ? "" : current.fieldName;

        StackNode peek = peek();
        String struct = current == null ? "" : current.optimizedStruct == null ? (peek.optimizedStruct == null ? "" : peek.optimizedStruct.struct.name) : current.optimizedStruct.struct.name;
        TException ex = new TException("JsonError, please check:"
                + struct + "." + fieldName);
        logger.error(ex.getMessage(), ex);
        throw ex;
    }

    private void logAndThrowTException(String msg) throws TException {
        TException ex = new TException("JsonError:" + msg);
        logger.error(ex.getMessage(), ex);
        throw ex;
    }

    /**
     * 用于保存当前处理节点的信息, 从之前的 immutable 调整为  mutable，并使用了一个简单的池，这样，StackNode
     * 的数量 = json的深度，而不是长度，降低内存需求
     */
    static class StackNode {

        private DataType dataType;
        /**
         * byteBuf position before this node created, maybe a Struct Field, or a Map field, or an array element
         */
        private int tFieldPosition;

        /**
         * byteBuf position after this node created
         */
        private int valuePosition;

        /**
         * optimizedStruct if dataType.kind==STRUCT
         */
        private OptimizedMetadata.OptimizedStruct optimizedStruct;

        /**
         * the field name
         */
        private String fieldName;

        /**
         * if datatype is optimizedStruct, all fieldMap parsed will be add to this set
         */
        private BitSet fields4Struct = new BitSet(64);

        /**
         * if dataType is a Collection(such as LIST, MAP, SET etc), elCount represents the size of the Collection.
         */
        int elCount = 0;

        boolean isNull = false;

        StackNode() {
        }

        public StackNode init(final DataType dataType, final int valuePosition, int tFieldPosition, final OptimizedMetadata.OptimizedStruct optimizedStruct, String fieldName) {
            this.dataType = dataType;
            this.valuePosition = valuePosition;
            this.tFieldPosition = tFieldPosition;
            this.optimizedStruct = optimizedStruct;
            this.fieldName = fieldName;

            this.fields4Struct.clear();
            this.elCount = 0;
            this.isNull = false;
            return this;
        }

        void incrElementSize() {
            elCount++;
        }

        public DataType getDataType() {
            return dataType;
        }

        public int gettFieldPosition() {
            return tFieldPosition;
        }

        public int getValuePosition() {
            return valuePosition;
        }

        public OptimizedMetadata.OptimizedStruct getOptimizedStruct() {
            return optimizedStruct;
        }

        public String getFieldName() {
            return fieldName;
        }

        public BitSet getFields4Struct() {
            return fields4Struct;
        }

    }
}
