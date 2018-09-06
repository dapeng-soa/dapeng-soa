package com.github.dapeng.json;


import com.github.dapeng.core.*;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

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
     * 标志是否是最外层object
     */
    boolean outsideBody = true;
    /**
     * onStartField的时候, 记录是否找到该Field. 如果没找到,那么需要skip这个field
     */
    boolean foundField = true;
    /**
     * 从第一个多余字段开始后,到该多余字段结束, 所解析到的字段的计数值.
     * 用于在多余字段是复杂结构类型的情况下, 忽略该复杂结构内嵌套的所有多余字段
     * 例如:
     * <pre>
     *  createdOrderRequest: {
     *      "buyerId":2,
     *      "sellerId":3,
     *      "uselessField1":{
     *          "innerField1":"a",
     *          "innerField2":"b"
     *      },
     *      "uselessField2":[
     *      {
     *          "innerField3":"c",
     *          "innerField4":"d"
     *      },
     *      {
     *          "innerField3":"c",
     *          "innerField4":"d"
     *      }]
     * }
     * </pre>
     * 当uselessField1跟uselessField2是多余字段时,我们要确保其内部所有字段都
     * 给skip掉.
     */
    int skipFieldsStack = 0;
    /**
     * 记录是否是null. 前端在处理optional字段的时候, 可能会传入一个null,见单元测试
     */
    boolean foundNull = true;

    /**
     * 是否遇到了最外层的body
     */
    boolean rootBodyFound = false;
    boolean rootBodyEnd = false;

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

        init();
    }

    private void init() {
        DataType initDataType = new DataType();
        initDataType.setKind(DataType.KIND.STRUCT);
        initDataType.qualifiedName = optimizedStruct.struct.name;
        current = new StackNode(initDataType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(),
                optimizedStruct, optimizedStruct.struct.name);
    }

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
     *  - 栈操作：将当前栈顶的子元素（valueType）类型压栈
     *  - 处理：proto.writeListBegin or proto.writeSetBegin
     * - onEndArray
     *  - 当前栈顶：
     *  - 栈操作：pop 恢复当前栈顶为 LIST/SET
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

    @Override
    public void onStartObject() throws TException {
        if (outsideBody) {
            // just skip the body level
            outsideBody = false;
            return;
        }

        if (!foundField) {
            skipFieldsStack++;
            return;
        }

        assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.increaseElement();
            //集合套集合的变态处理方式  todo
            current = new StackNode(peek.dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), current.optimizedStruct, current.optimizedStruct == null ? null : current.optimizedStruct.struct.name);
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
        if (rootBodyEnd) return;

        if (!foundField) {
            skipFieldsStack--;
            return;
        }

        assert skipFieldsStack == 0;

        assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

        switch (current.dataType.kind) {
            case STRUCT:
                validateStruct(current);

                oproto.writeFieldStop();
                oproto.writeStructEnd();
                if (current.optimizedStruct.struct.name.equals(optimizedStruct.struct.name)) {
                    rootBodyEnd = true;
                }
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
        if (!foundField) {
            skipFieldsStack++;
            return;
        }

        assert isCollectionKind(current.dataType.kind);

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.increaseElement();
            //集合套集合的变态处理方式 todo
            current = new StackNode(peek.dataType.valueType, requestByteBuf.writerIndex(),
                    requestByteBuf.writerIndex(), current.optimizedStruct,
                    current.optimizedStruct == null ? null : current.optimizedStruct.struct.name);
        }

        switch (current.dataType.kind) {
            case LIST:
            case SET:
                writeCollectionBegin(dataType2Byte(current.dataType.valueType), 0);
                break;
            default:
                logAndThrowTException();
        }

        OptimizedMetadata.OptimizedStruct nextStruct = optimizedService.optimizedStructs.get(current.dataType.valueType.qualifiedName);
        stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(),
                requestByteBuf.writerIndex(), nextStruct, nextStruct == null ? "" : nextStruct.struct.name));
    }

    @Override
    public void onEndArray() throws TException {
        if (!foundField) {
            skipFieldsStack--;
            return;
        }

        pop();

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
        if (!foundField || rootBodyEnd) {
            return;
        }

        if (rootBodyFound && current.dataType.kind == DataType.KIND.MAP) {
            assert isValidMapKeyType(current.dataType.keyType.kind);
            // key有可能是String, 也有可能是Int
            if (current.dataType.keyType.kind == DataType.KIND.STRING) {
                oproto.writeString(name);
            } else {
                writeIntField(name, current.dataType.keyType.kind);
            }
            stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(),
                    requestByteBuf.writerIndex(),
                    optimizedService.optimizedStructs.get(current.dataType.valueType.qualifiedName), name));
        } else {
            // reset field status
            foundNull = false;
            foundField = true;

            if (rootBodyFound) {
                Field field = current.optimizedStruct.fieldMap.get(name);
                if (field == null) {
                    foundField = false;
                    logger.debug("field(" + name + ") not found. just skip");
                    return;
                }

                if (current.dataType.kind == DataType.KIND.STRUCT) {
                    current.fields4Struct.add(name);
                }

                int byteBufPositionBefore = requestByteBuf.writerIndex();
                oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
                stackNew(new StackNode(field.dataType, requestByteBuf.writerIndex(),
                        byteBufPositionBefore, optimizedService.optimizedStructs.get(field.dataType.qualifiedName), name));
            } else if (!rootBodyFound && !"body".equals(name)) {
                logAndThrowTException("no body");
            } else if (!rootBodyFound && "body".equals(name)) {
                rootBodyFound = true;
            }
        }

    }

    @Override
    public void onEndField() throws TException {
        if (!foundField || rootBodyEnd) {
            // reset the flag
            if (skipFieldsStack == 0) {
                foundField = true;
            }
            return;
        }

        String fieldName = current.fieldName;
        pop();

        if (foundNull) {
            if (current.dataType.kind == DataType.KIND.STRUCT) {
                current.fields4Struct.remove(fieldName);
            }
        }
        if (current.dataType.kind != DataType.KIND.MAP && !foundNull) {
            oproto.writeFieldEnd();
        }

        foundNull = false;
    }

    @Override
    public void onBoolean(boolean value) throws TException {
        if (!foundField) {
            return;
        }

        if (current.dataType.kind != DataType.KIND.BOOLEAN) {
            logAndThrowTException();
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.increaseElement();
        }

        oproto.writeBool(value);
    }

    @Override
    public void onNumber(double value) throws TException {
        DataType.KIND currentType = current.dataType.kind;

        if (!foundField) {
            return;
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.increaseElement();
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

        if (!foundField) {
            return;
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.increaseElement();
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
        if (!foundField) {
            return;
        }
        foundNull = true;
        //reset writerIndex, skip the field
        requestByteBuf.writerIndex(current.byteBufPositionBefore);
        if (invocationCtx.codecProtocol() == CompressedBinary) {
            ((TCompactProtocol) oproto).resetLastFieldId();
        }
    }

    @Override
    public void onString(String value) throws TException {
        if (!foundField) {
            return;
        }

        StackNode peek = peek();
        if (peek != null && isMultiElementKind(peek.dataType.kind)) {
            peek.increaseElement();
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

    private void stackNew(StackNode node) {
        history.push(this.current);
        this.current = node;
    }

    private StackNode pop() {
        return this.current = history.pop();
    }

    private StackNode peek() {
        return history.empty() ? null : history.peek();
    }


    private void validateStruct(StackNode current) throws TException {
        /**
         * 不在该Struct必填字段列表的字段列表
         */
        for (Field field : current.optimizedStruct.fieldMapByTag.values()) {
            if (field != null && !field.isOptional() && !current.fields4Struct.contains(field.name)) {
                String fieldName = current.fieldName;
                String struct = current.optimizedStruct.struct.name;
                SoaException ex = new SoaException(SoaCode.ReqFieldNull.getCode(), "JsonError, please check:"
                        + struct + "." + fieldName
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
                oproto.writeI16(Short.valueOf(value));
                break;
            case INTEGER:
                oproto.writeI32(Integer.valueOf(value));
                break;
            case LONG:
                oproto.writeI64(Long.valueOf(value));
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
        int beginPosition = current.byteBufPosition;
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
     * 用于保存当前处理节点的信息
     */
    static class StackNode {
        final DataType dataType;
        /**
         * byteBuf position after this node created
         */
        final int byteBufPosition;

        /**
         * byteBuf position before this node created
         */
        final int byteBufPositionBefore;

        /**
         * optimizedStruct if dataType.kind==STRUCT
         */
        final OptimizedMetadata.OptimizedStruct optimizedStruct;

        /**
         * the field name
         */
        final String fieldName;

        /**
         * if datatype is optimizedStruct, all fieldMap parsed will be add to this set
         */
        final Set<String> fields4Struct = new HashSet<>(32);

        /**
         * if dataType is a Collection(such as LIST, MAP, SET etc), elCount represents the size of the Collection.
         */
        private int elCount = 0;

        StackNode(final DataType dataType, final int byteBufPosition, int byteBufPositionBefore, final OptimizedMetadata.OptimizedStruct optimizedStruct, String fieldName) {
            this.dataType = dataType;
            this.byteBufPosition = byteBufPosition;
            this.byteBufPositionBefore = byteBufPositionBefore;
            this.optimizedStruct = optimizedStruct;
            this.fieldName = fieldName;
        }

        void increaseElement() {
            elCount++;
        }
    }
}
