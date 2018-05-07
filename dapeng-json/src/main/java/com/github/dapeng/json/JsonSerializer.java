package com.github.dapeng.json;

import com.github.dapeng.core.*;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.dapeng.core.enums.CodecProtocol.CompressedBinary;
import static com.github.dapeng.util.MetaDataUtil.*;

/**
 * @author ever
 */
public class JsonSerializer implements BeanSerializer<String> {
    private final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private final Struct struct;
    private ByteBuf requestByteBuf;
    private final Service service;
    private final Method method;
    private final InvocationContext invocationCtx = InvocationContextImpl.Factory.getCurrentInstance();

    public JsonSerializer(Service service, Method method, Struct struct) {
        this.struct = struct;
        this.service = service;
        this.method = method;
    }

    // thrift -> json
    private void read(TProtocol iproto, JsonCallback writer) throws TException {
        iproto.readStructBegin();
        writer.onStartObject();

        while (true) {
            TField field = iproto.readFieldBegin();
            if (field.type == TType.STOP)
                break;

            List<Field> flds = struct.getFields().stream().filter(element -> element.tag == field.id)
                    .collect(Collectors.toList());

            Field fld = flds.isEmpty() ? null : flds.get(0);

            boolean skip = fld == null;


            if (!skip) {
                writer.onStartField(fld.name);
                readField(iproto, fld.dataType, field.type, writer, skip);
                writer.onEndField();
            }

            iproto.readFieldEnd();
        }


        iproto.readStructEnd();
        writer.onEndObject();
    }

    private void readField(TProtocol iproto, DataType fieldDataType, byte fieldType,
                           JsonCallback writer, boolean skip) throws TException {
        switch (fieldType) {
            case TType.VOID:
                break;
            case TType.BOOL:
                boolean boolValue = iproto.readBool();
                if (!skip) {
                    writer.onBoolean(boolValue);
                }
                break;
            case TType.BYTE:
                // TODO
                byte b = iproto.readByte();
                if (!skip) {
                    writer.onNumber(b);
                }
                break;
            case TType.DOUBLE:
                double dValue = iproto.readDouble();
                if (!skip) {
                    writer.onNumber(dValue);
                }
                break;
            case TType.I16:
                short sValue = iproto.readI16();
                if (!skip) {
                    writer.onNumber(sValue);
                }
                break;
            case TType.I32:
                int iValue = iproto.readI32();
                if (!skip) {
                    if (fieldDataType != null && fieldDataType.kind == DataType.KIND.ENUM) {
                        String enumLabel = findEnumItemLabel(findEnum(fieldDataType.qualifiedName, service), iValue);
                        writer.onString(enumLabel);
                    } else {
                        writer.onNumber(iValue);
                    }
                }
                break;
            case TType.I64:
                long lValue = iproto.readI64();
                if (!skip) {
                    writer.onNumber(lValue);
                }
                break;
            case TType.STRING:
                String strValue = iproto.readString();
                if (!skip) {
                    writer.onString(strValue);
                }
                break;
            case TType.STRUCT:
                if (!skip) {
                    String subStructName = fieldDataType.qualifiedName;
                    Struct subStruct = findStruct(subStructName, service);
                    new JsonSerializer(service, method, subStruct).read(iproto, writer);
                } else {
                    TProtocolUtil.skip(iproto, TType.STRUCT);
                }
                break;
            case TType.MAP:
                if (!skip) {
                    TMap map = iproto.readMapBegin();
                    writer.onStartObject();
                    for (int index = 0; index < map.size; index++) {
                        switch (map.keyType) {
                            case TType.STRING:
                                writer.onStartField(iproto.readString());
                                break;
                            case TType.I16:
                                writer.onStartField(String.valueOf(iproto.readI16()));
                                break;
                            case TType.I32:
                                writer.onStartField(String.valueOf(iproto.readI32()));
                                break;
                            case TType.I64:
                                writer.onStartField(String.valueOf(iproto.readI64()));
                                break;
                            default:
                                logger.error("won't be here", new Throwable());
                        }

                        readField(iproto, fieldDataType.valueType, map.valueType, writer, false);
                        writer.onEndField();
                    }
                    writer.onEndObject();
                } else {
                    TProtocolUtil.skip(iproto, TType.MAP);
                }
                break;
            case TType.SET:
                if (!skip) {
                    TSet set = iproto.readSetBegin();
                    writer.onStartArray();
                    readCollection(set.size, set.elemType, fieldDataType.valueType, fieldDataType.valueType.valueType, iproto, writer);
                    writer.onEndArray();
                } else {
                    TProtocolUtil.skip(iproto, TType.SET);
                }
                break;
            case TType.LIST:
                if (!skip) {
                    TList list = iproto.readListBegin();
                    writer.onStartArray();
                    readCollection(list.size, list.elemType, fieldDataType.valueType, fieldDataType.valueType.valueType, iproto, writer);
                    writer.onEndArray();
                } else {
                    TProtocolUtil.skip(iproto, TType.LIST);
                }
                break;
            default:

        }
    }

    /**
     * @param size
     * @param elemType     thrift的数据类型
     * @param metadataType metaData的DataType
     * @param iproto
     * @param writer
     * @throws TException
     */
    private void readCollection(int size, byte elemType, DataType metadataType, DataType subMetadataType, TProtocol iproto, JsonCallback writer) throws TException {
        Struct struct = null;
        if (metadataType.kind == DataType.KIND.STRUCT) {
            struct = findStruct(metadataType.qualifiedName, service);
        }
        for (int index = 0; index < size; index++) {
            if (!isComplexKind(metadataType.kind)) {//没有嵌套结构,也就是原始数据类型, 例如int, boolean,string等
                readField(iproto, metadataType, elemType, writer, false);
            } else {
                if (struct != null) {
                    new JsonSerializer(service, method, struct).read(iproto, writer);
                } else if (isCollectionKind(metadataType.kind)) {
                    //处理List<list<>>
                    TList list = iproto.readListBegin();
                    writer.onStartArray();
                    readCollection(list.size, list.elemType, subMetadataType, subMetadataType.valueType, iproto, writer);
                    writer.onEndArray();
                } else if (metadataType.kind == DataType.KIND.MAP) {
                    readField(iproto, metadataType, elemType, writer, false);
                }
            }
            writer.onEndField();
        }

    }

    @Override
    public String read(TProtocol iproto) throws TException {

        JsonWriter writer = new JsonWriter();
        read(iproto, writer);
        return writer.toString();
    }

    /**
     * format:
     * url:http://xxx/api/callService?serviceName=xxx&version=xx&method=xx
     * post body:
     * {
     * "header":{},
     * "body":{
     * ${structName}:{}
     * }
     * }
     * <p>
     * InvocationContext and SoaHeader should be ready before
     */
    enum ParsePhase {
        INIT, HEADER_BEGIN, HEADER, HEADER_END, BODY_BEGIN, BODY, BODY_END
    }

    class Json2ThriftCallback implements JsonCallback {
        /**
         * 压缩二进制编码下,集合的默认长度(占3字节)
         */
        private final TProtocol oproto;
        private ParsePhase parsePhase = ParsePhase.INIT;

        /**
         * 用于保存当前处理节点的信息
         */
        class StackNode {
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
             * struct if dataType.kind==STRUCT
             */
            final Struct struct;

            /**
             * the field name
             */
            final String fieldName;

            /**
             * if datatype is struct, all fields parsed will be add to this set
             */
            final Set<String> fields4Struct = new HashSet<>(32);

            /**
             * if dataType is a Collection(such as LIST, MAP, SET etc), elCount represents the size of the Collection.
             */
            private int elCount = 0;

            StackNode(final DataType dataType, final int byteBufPosition, int byteBufPositionBefore, final Struct struct, String fieldName) {
                this.dataType = dataType;
                this.byteBufPosition = byteBufPosition;
                this.byteBufPositionBefore = byteBufPositionBefore;
                this.struct = struct;
                this.fieldName = fieldName;
            }

            void increaseElement() {
                elCount++;
            }
        }

        //当前处理数据节点
        StackNode current;
        String currentHeaderName;
        //onStartField的时候, 记录是否找到该Field. 如果没找到,那么需要skip这个field
        boolean foundField = true;
        /**
         * todo
         * 从第一个多余字段开始后,到该多余字段结束, 所解析到的字段的计数值.
         * 用于在多余字段是复杂结构类型的情况下, 忽略该复杂结构内嵌套的所有多余字段
         * 例如:
         * {
         * createdOrderRequest: {
         * "buyerId":2,
         * "sellerId":3,
         * "uselessField1":{
         * "innerField1":"a",
         * "innerField2":"b"
         * },
         * "uselessField2":[{
         * "innerField3":"c",
         * "innerField4":"d"
         * },
         * {
         * "innerField3":"c",
         * "innerField4":"d"
         * }]
         * }
         * }
         * 当uselessField1跟uselessField2是多余字段时,我们要确保其内部所有字段都
         * 给skip掉.
         */
        int skipFieldsStack = 0;
        //记录是否是null. 前端在处理optional字段的时候, 可能会传入一个null,见单元测试
        boolean foundNull = true;

        /**
         * @param oproto
         */
        Json2ThriftCallback(TProtocol oproto) {
            this.oproto = oproto;
        }


        /*  {  a:, b:, c: { ... }, d: [ { }, { } ]  }
         *
         *  init                -- [], topStruct
         *  onStartObject
         *    onStartField a    -- [topStruct], DataType a
         *    onEndField a
         *    ...
         *    onStartField c    -- [topStruct], StructC
         *      onStartObject
         *          onStartField
         *          onEndField
         *          ...
         *      onEndObject     -- [], topStruct
         *    onEndField c
         *
         *    onStartField d
         *      onStartArray    -- [topStruct] structD
         *          onStartObject
         *          onEndObject
         *      onEndArray      -- []
         *    onEndField d
         */
        Stack<StackNode> history = new Stack<>();

        @Override
        public void onStartObject() throws TException {
            switch (parsePhase) {
                case INIT:
                    break;
                case HEADER_BEGIN:
                    parsePhase = ParsePhase.HEADER;
                    break;
                case HEADER:
                    logAndThrowTException();
                case HEADER_END:
                    break;
                case BODY_BEGIN:
                    new SoaHeaderSerializer().write(buildSoaHeader(), new TBinaryProtocol(oproto.getTransport()));

                    //初始化当前数据节点
                    DataType initDataType = new DataType();
                    initDataType.setKind(DataType.KIND.STRUCT);
                    initDataType.qualifiedName = struct.name;
                    current = new StackNode(initDataType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), struct, struct.name);

                    oproto.writeStructBegin(new TStruct(current.struct.name));

                    parsePhase = ParsePhase.BODY;
                    break;
                case BODY:
                    if (!foundField) {
                        skipFieldsStack++;
                        return;
                    }

                    assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                        peek().increaseElement();
                        //集合套集合的变态处理方式
                        current = new StackNode(peek().dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), current.struct, current.struct == null ? null : current.struct.name);
                    }
                    switch (current.dataType.kind) {
                        case STRUCT:
                            Struct struct = current.struct;//findStruct(current.dataType.qualifiedName, service);
                            if (struct == null) {
                                logger.error("struct not found");
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
                    break;
                default:
                    logAndThrowTException();
            }
        }

        @Override
        public void onEndObject() throws TException {
            switch (parsePhase) {
                case HEADER_BEGIN:
                    logAndThrowTException();
                    break;
                case HEADER:
                    parsePhase = ParsePhase.HEADER_END;
                    break;
                case HEADER_END:
                    logAndThrowTException();
                    break;
                case BODY_BEGIN:
                    logAndThrowTException();
                    break;
                case BODY:
                    if (!foundField) {
                        skipFieldsStack--;
                        return;
                    } else {
                        assert skipFieldsStack == 0;
                    }

                    assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

                    switch (current.dataType.kind) {
                        case STRUCT:
                            validateStruct(current);

                            oproto.writeFieldStop();
                            oproto.writeStructEnd();
                            if (current.struct.name.equals(struct.name)) {
                                parsePhase = ParsePhase.BODY_END;
                            }
                            break;
                        case MAP:
                            oproto.writeMapEnd();

                            reWriteByteBuf();
                            break;
                        default:
                            logAndThrowTException();
                    }
                    break;
                case BODY_END:
                    break;
                default:
                    logAndThrowTException();
            }
        }

        private void validateStruct(StackNode current) throws TException {
            /**
             * 不在该Struct必填字段列表的字段列表
             */
            List<Field> mandatoryMissFileds = current.struct.fields.stream()
                    .filter(field -> !field.isOptional())
                    .filter(field -> !current.fields4Struct.contains(field.name))
                    .collect(Collectors.toList());
            if (!mandatoryMissFileds.isEmpty()) {
                String fieldName = current.fieldName;
                String struct = current.struct.name;
                TException ex = new TException("JsonError, please check:"
                        + struct + "." + fieldName
                        + ", struct mandatory fields missing:"
                        + mandatoryMissFileds.stream().map(field -> field.name + ", ").collect(Collectors.toList()));
                logger.error(ex.getMessage(), ex);
                throw ex;
            }
        }

        /**
         * 由于目前拿不到集合的元素个数, 暂时设置为0个
         *
         * @throws TException
         */
        @Override
        public void onStartArray() throws TException {
            if (parsePhase != ParsePhase.BODY || !foundField) {
                if (!foundField) {
                    skipFieldsStack++;
                }
                return;
            }

            assert isCollectionKind(current.dataType.kind);

            if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                peek().increaseElement();
                //集合套集合的变态处理方式
                current = new StackNode(peek().dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), current.struct, current.struct == null ? null : current.struct.name);
            }

            switch (current.dataType.kind) {
                case LIST:
                case SET:
                    writeCollectionBegin(dataType2Byte(current.dataType.valueType), 0);
                    break;
                default:
                    logAndThrowTException();
            }

            Struct nextStruct = findStruct(current.dataType.valueType.qualifiedName, service);
            stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), nextStruct, nextStruct == null ? "" : nextStruct.name));
        }

        @Override
        public void onEndArray() throws TException {
            if (parsePhase != ParsePhase.BODY || !foundField) {
                if (!foundField) {
                    skipFieldsStack--;
                }
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
            switch (parsePhase) {
                case INIT:
                    if ("header".equals(name)) {
                        parsePhase = ParsePhase.HEADER_BEGIN;
                    } else if ("body".equals(name)) {
                        parsePhase = ParsePhase.BODY_BEGIN;
                    } else {
                        logger.warn("skip field(" + name + ")@pase:" + parsePhase);
                    }
                    break;
                case HEADER:
                    currentHeaderName = name;
                    break;
                case HEADER_END:
                    if ("body".equals(name)) {
                        parsePhase = ParsePhase.BODY_BEGIN;
                    } else {
                        logger.warn("skip field(" + name + ")@pase:" + parsePhase);
                    }
                    break;
                case BODY:
                    if (!foundField) {
                        return;
                    }

                    if (current.dataType.kind == DataType.KIND.MAP) {
                        assert isValidMapKeyType(current.dataType.keyType.kind);
                        stackNew(new StackNode(current.dataType.keyType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), null, name));
                        // key有可能是String, 也有可能是Int
                        if (current.dataType.kind == DataType.KIND.STRING) {
                            oproto.writeString(name);
                        } else {
                            writeIntField(name, current.dataType.kind);
                        }
                        pop();
                        stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(), requestByteBuf.writerIndex(), findStruct(current.dataType.valueType.qualifiedName, service), name));
                    } else {
                        // reset field status
                        foundNull = false;
                        foundField = true;
                        Field field = findField(name, current.struct);
                        if (field == null) {
                            foundField = false;
                            logger.info("field(" + name + ") not found. just skip");
                            return;
                        }

                        if (current.dataType.kind == DataType.KIND.STRUCT) {
                            current.fields4Struct.add(name);
                        }

                        int byteBufPositionBefore = requestByteBuf.writerIndex();
                        oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
                        stackNew(new StackNode(field.dataType, requestByteBuf.writerIndex(), byteBufPositionBefore, findStruct(field.dataType.qualifiedName, service), name));
                    }
                    break;
                case BODY_END:
                    logger.warn("skip field(" + name + ")@pase:" + parsePhase);
                    break;
                default:
                    logAndThrowTException();
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

        @Override
        public void onEndField() throws TException {
            if (parsePhase != ParsePhase.BODY || !foundField) {
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
            switch (parsePhase) {
                case HEADER:
                    logger.warn("skip boolean(" + value + ")@pase:" + parsePhase + " field:" + current.fieldName);
                    break;
                case BODY:
                    if (!foundField) {
                        return;
                    }
                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                        peek().increaseElement();
                    }

                    oproto.writeBool(value);
                    break;
                default:
                    logger.warn("skip boolean(" + value + ")@pase:" + parsePhase + " for field:" + current.fieldName);
            }

        }

        @Override
        public void onNumber(double value) throws TException {
            switch (parsePhase) {
                case HEADER:
                    fillIntToInvocationCtx((int) value);
                    break;
                case BODY:
                    DataType.KIND currentType = current.dataType.kind;

                    if (!foundField) {
                        return;
                    }

                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                        peek().increaseElement();
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
                            throw new TException("Field:" + current.fieldName + ", DataType(" + current.dataType.kind + ") for " + current.dataType.qualifiedName + " is not a Number");

                    }
                    break;
                default:
                    logger.warn("skip number(" + value + ")@pase:" + parsePhase + " Field:" + current.fieldName);
            }
        }

        @Override
        public void onNumber(long value) throws TException {
            switch (parsePhase) {
                case HEADER:
                    fillIntToInvocationCtx((int) value);
                    break;
                case BODY:
                    DataType.KIND currentType = current.dataType.kind;

                    if (!foundField) {
                        return;
                    }

                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                        peek().increaseElement();
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
                            throw new TException("Field:" + current.fieldName + ", DataType(" + current.dataType.kind + ") for " + current.dataType.qualifiedName + " is not a Number");

                    }
                    break;
                default:
                    logger.warn("skip number(" + value + ")@pase:" + parsePhase + " Field:" + current.fieldName);
            }
        }

        @Override
        public void onNull() throws TException {
            switch (parsePhase) {
                case HEADER:
                    break;
                case BODY:
                    if (!foundField) {
                        return;
                    }
                    foundNull = true;
                    //reset writerIndex, skip the field
                    requestByteBuf.writerIndex(current.byteBufPositionBefore);
                    if (invocationCtx.getCodecProtocol() == CompressedBinary) {
                        ((TCompactProtocol)oproto).resetLastFieldId();
                    }
                    break;
                default:
                    logAndThrowTException();
            }
        }

        @Override
        public void onString(String value) throws TException {
            switch (parsePhase) {
                case HEADER:
                    fillStringToInvocationCtx(value);
                    break;
                case BODY:
                    if (!foundField) {
                        return;
                    }

                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                        peek().increaseElement();
                    }

                    switch (current.dataType.kind) {
                        case ENUM:
                            TEnum tEnum = findEnum(current.dataType.qualifiedName, service);
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
                            oproto.writeI64(Long.valueOf(value));
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


                    break;
                default:
                    logger.warn("skip boolean(" + value + ")@pase:" + parsePhase + " Field:" + current.fieldName);
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
                    && invocationCtx.getCodecProtocol() == CompressedBinary
                    && elCount == 0) {
                requestByteBuf.writerIndex(beginPosition + 1);
            } else {
                requestByteBuf.writerIndex(currentIndex);
            }
        }

        private void writeMapBegin(byte keyType, byte valueType, int defaultSize) throws TException {
            switch (invocationCtx.getCodecProtocol()) {
                case Binary:
                    oproto.writeMapBegin(new TMap(keyType, valueType, defaultSize));
                    break;
                case CompressedBinary:
                default:
                    TJsonCompressProtocolUtil.writeMapBegin(keyType, valueType, requestByteBuf);
                    break;
            }
        }

        private void reWriteMapBegin(byte keyType, byte valueType, int size) throws TException {
            switch (invocationCtx.getCodecProtocol()) {
                case Binary:
                    oproto.writeMapBegin(new TMap(keyType, valueType, size));
                    break;
                case CompressedBinary:
                default:
                    TJsonCompressProtocolUtil.reWriteMapBegin(size, requestByteBuf);
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
            switch (invocationCtx.getCodecProtocol()) {
                case Binary:
                    oproto.writeListBegin(new TList(valueType, defaultSize));
                    break;
                case CompressedBinary:
                default:
                    TJsonCompressProtocolUtil.writeCollectionBegin(valueType, requestByteBuf);
                    break;
            }
        }

        private void reWriteCollectionBegin(byte valueType, int size) throws TException {
            switch (invocationCtx.getCodecProtocol()) {
                case Binary:
                    oproto.writeListBegin(new TList(valueType, size));
                    break;
                case CompressedBinary:
                default:
                    TJsonCompressProtocolUtil.reWriteCollectionBegin(size, requestByteBuf);
                    break;
            }
        }

        private void fillStringToInvocationCtx(String value) {
            if ("serviceName".equals(currentHeaderName)) {
                invocationCtx.setServiceName(value);
            } else if ("methodName".equals(currentHeaderName)) {
                invocationCtx.setMethodName(value);
            } else if ("versionName".equals(currentHeaderName)) {
                invocationCtx.setVersionName(value);
            } else if ("calleeIp".equals(currentHeaderName)) {
                invocationCtx.setCalleeIp(Optional.of(value));
            } else if ("callerFrom".equals(currentHeaderName)) {
                invocationCtx.setCallerFrom(Optional.of(value));
            } else if ("callerIp".equals(currentHeaderName)) {
                invocationCtx.setCallerFrom(Optional.of(value));
            } else if ("customerName".equals(currentHeaderName)) {
                invocationCtx.setCustomerName(Optional.of(value));
            } else {
                logger.warn("skip field(" + currentHeaderName + ")@pase:" + parsePhase);
            }
        }


        private void logAndThrowTException() throws TException {
            String fieldName = current == null ? "" : current.fieldName;
            String struct = current == null ? "" : current.struct == null ? (peek().struct == null ? "" : peek().struct.name) : current.struct.name;
            TException ex = new TException("JsonError, please check:"
                    + struct + "." + fieldName
                    + ", current phase:" + parsePhase);
            logger.error(ex.getMessage(), ex);
            throw ex;
        }

        private SoaHeader buildSoaHeader() {
            SoaHeader header = new SoaHeader();
            header.setServiceName(invocationCtx.getServiceName());
            header.setVersionName(invocationCtx.getVersionName());
            header.setMethodName(invocationCtx.getMethodName());

            header.setCallerFrom(invocationCtx.getCallerFrom());
            header.setCallerIp(invocationCtx.getCallerIp());
            header.setCustomerId(invocationCtx.getCustomerId());
            header.setCustomerName(invocationCtx.getCustomerName());
            header.setOperatorId(invocationCtx.getOperatorId());
            header.setOperatorName(invocationCtx.getOperatorName());
            header.setTransactionId(invocationCtx.getTransactionId());


            header.setTransactionId(invocationCtx.getTransactionId());

            return header;
        }

        private void fillIntToInvocationCtx(int value) {
            if ("calleePort".equals(currentHeaderName)) {
                invocationCtx.setCalleePort(Optional.of(value));
            } else if ("operatorId".equals(currentHeaderName)) {
                invocationCtx.setOperatorId(Optional.of(value));
            } else if ("customerId".equals(currentHeaderName)) {
                invocationCtx.setCustomerId(Optional.of(value));
            } else if ("transactionSequence".equals(currentHeaderName)) {
                invocationCtx.setTransactionSequence(Optional.of(value));
            } else {
                logger.warn("skip field(" + currentHeaderName + ")@pase:" + parsePhase);
            }
        }
    }


    // json -> thrift

    /**
     * {
     * header:{
     * <p>
     * },
     * boday:{
     * ${struct.name}:{
     * <p>
     * }
     * }
     * }
     *
     * @param input
     * @param oproto
     * @throws TException
     */
    @Override
    public void write(String input, TProtocol oproto) throws TException {
        Json2ThriftCallback j2tCallback = new Json2ThriftCallback(oproto);
        try {
            new JsonParser(input, j2tCallback).parseJsValue();
        } catch (RuntimeException e) {
            if (j2tCallback.current != null) {
                String errorMsg = "Please check field:" + j2tCallback.current.fieldName;
                logger.error(errorMsg + "\n" + e.getMessage(), e);
                throw new TException(errorMsg);
            }
            throw e;
        }
    }

    @Override
    public void validate(String s) throws TException {

    }

    @Override
    public String toString(String s) {
        return s;
    }

    /**
     * 暂时只支持key为整形或者字符串的map
     *
     * @param kind
     * @return
     */
    private boolean isValidMapKeyType(DataType.KIND kind) {
        return kind == DataType.KIND.INTEGER || kind == DataType.KIND.LONG
                || kind == DataType.KIND.SHORT || kind == DataType.KIND.STRING;
    }

    /**
     * 是否集合类型
     *
     * @param kind
     * @return
     */
    private boolean isCollectionKind(DataType.KIND kind) {
        return kind == DataType.KIND.LIST || kind == DataType.KIND.SET;
    }

    /**
     * 是否容器类型
     *
     * @param kind
     * @return
     */
    private boolean isMultiElementKind(DataType.KIND kind) {
        return isCollectionKind(kind) || kind == DataType.KIND.MAP;
    }

    /**
     * 是否复杂类型
     *
     * @param kind
     * @return
     */
    private boolean isComplexKind(DataType.KIND kind) {
        return isMultiElementKind(kind) || kind == DataType.KIND.STRUCT;
    }

    public void setRequestByteBuf(ByteBuf requestByteBuf) {
        this.requestByteBuf = requestByteBuf;
    }
}

/**
 * dapeng-json定制的Thrift二进制压缩协议工具类,主要更改了集合类的序列化方法
 *
 * @author ever
 */
class TJsonCompressProtocolUtil {

    /**
     * Invoked before we get the actually collection size.
     * Always assume that the collection size should take 3 bytes.
     *
     * @param elemType type of the collection Element
     * @throws TException
     */
    public static void writeCollectionBegin(byte elemType, ByteBuf byteBuf) throws TException {
        writeByteDirect((byte) (0xf0 | TCompactProtocol.ttypeToCompactType[elemType]), byteBuf);
        //write 3 byte with 0x0 to hold the collectionSize
        writeFixedLengthVarint32(3, byteBuf);
    }

    /**
     * Invoked after we get the actually collection size.
     *
     * @param size    collection size
     * @param byteBuf
     * @throws TException
     */
    public static void reWriteCollectionBegin(int size, ByteBuf byteBuf) throws TException {
        //Actually we should only change the collection length.
        byteBuf.writerIndex(byteBuf.writerIndex() + 1);
        reWriteVarint32(size, byteBuf);
    }

    /**
     * Invoked before we get the actually collection size.
     * Always assume that the collection size should take 3 bytes.
     *
     * @throws TException
     */
    public static void writeMapBegin(byte keyType, byte valueType, ByteBuf byteBuf) throws TException {
        /**
         * origin implementation:
         *  if (map.size == 0) {
         *     writeByteDirect(0);
         *  } else {
         *   writeVarint32(map.size);
         *   writeByteDirect(getCompactType(map.keyType) << 4 | getCompactType(map.valueType));
         *  }
         */
        writeFixedLengthVarint32(3, byteBuf);
        writeByteDirect((byte) (TCompactProtocol.ttypeToCompactType[keyType] << 4
                | TCompactProtocol.ttypeToCompactType[valueType]), byteBuf);
    }

    /**
     * Invoked after we get the actually collection size.
     * Please note that writerIndex should be updated:
     * 1. size > 0, byteBuf.writerIndex(currentWriterIndex);
     * 2. size = 0, byteBuf.writerIndex(byteBuf.writerIndex() + 1);
     *
     * @param size    collection size
     * @param byteBuf byteBuf which has reset the writerIndex to before collection
     * @throws TException
     */
    public static void reWriteMapBegin(int size, ByteBuf byteBuf) throws TException {
        if (size > 0) {
            reWriteVarint32(size, byteBuf);
        } else {
            // just need to update the byteBuf writerIndex by caller.
        }
    }

    private static byte[] byteDirectBuffer = new byte[1];

    private static void writeByteDirect(byte b, ByteBuf byteBuf) throws TException {
        byteDirectBuffer[0] = b;
        byteBuf.writeBytes(byteDirectBuffer);
    }

    /**
     * write count bytes as placeholder
     *
     * @param count   count of bytes
     * @param byteBuf
     * @throws TException
     */
    private static void writeFixedLengthVarint32(int count, ByteBuf byteBuf) throws TException {
        for (int i = 0; i < count; i++) {
            writeByteDirect((byte) 0, byteBuf);
        }
    }

    /**
     * 低位在前, 高位在后,
     * n = 3, result = 0x83 0x80 0x00
     * n = 129(1000 0001), result = 0x81 0x81 0x00
     * n = 130(1000 0010), result = 0x82 0x81 0x00
     * n = 65537(1 0000 0000 0000 0001) result = 0x81 0x80 0x04
     * Write an i32 as a varint. Always results in 3 bytes on the wire.
     */
    private static byte[] i32buf = new byte[3];

    private static void reWriteVarint32(int n, ByteBuf byteBuf) throws TException {
        int idx = 0;
        while (true) {
            if (idx >= i32buf.length) {
                throw new TException("Too long:" + n);
            }

            if ((n & ~0x7F) == 0) {
                i32buf[idx++] = (byte) n;
                break;
            } else {
                i32buf[idx++] = (byte) ((n & 0x7F) | 0x80);
                n >>>= 7;
            }
        }

        // 如果不够位数, 那么最后一个首位需要置1,说明后续还有数字.
        if (idx < i32buf.length) {
            i32buf[idx - 1] |= 0x80;
        }

        byteBuf.writeBytes(i32buf, 0, idx);

        for (int i = idx; i < i32buf.length - 1; i++) {
            byteBuf.writeByte((byte) 0x80);
        }

        if (idx < i32buf.length) {
            byteBuf.writeByte((byte) 0x00);
        }
    }
}
