package com.github.dapeng.json;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.metadata.DataType;
import com.github.dapeng.core.metadata.Field;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.dapeng.json.JsonUtils.isCollectionKind;
import static com.github.dapeng.json.JsonUtils.isComplexKind;
import static com.github.dapeng.util.MetaDataUtil.findEnumItemLabel;

/**
 * @author ever
 */
public class JsonSerializer implements BeanSerializer<String> {
    private final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private final OptimizedMetadata.OptimizedStruct optimizedStruct;
    private final OptimizedMetadata.OptimizedService optimizedService;
    private final Method method;
    private final String version;
    private ByteBuf requestByteBuf;

    public JsonSerializer(OptimizedMetadata.OptimizedService optimizedService,
                          Method method, String version,
                          OptimizedMetadata.OptimizedStruct optimizedStruct) {
        this.optimizedStruct = optimizedStruct;
        this.optimizedService = optimizedService;
        this.method = method;
        this.version = version;
    }

    /**
     * thrift -> json
     * @param iproto
     * @return
     * @throws TException
     */
    @Override
    public String read(TProtocol iproto) throws TException {

        JsonWriter writer = new JsonWriter();
        read(iproto, writer);
        return writer.toString();
    }


    /**
     *
     * json -> thrift
     * @param input
     * @param oproto
     * @throws TException
     */
    @Override
    public void write(String input, TProtocol oproto) throws TException {
        JsonReader jsonReader = new JsonReader(optimizedStruct, optimizedService, requestByteBuf, oproto);
        try {
            new JsonParser(input, jsonReader).parseJsValue();
        } catch (RuntimeException e) {
            if (jsonReader.current != null) {
                String errorMsg = "Please check field:" + jsonReader.current.getFieldName();
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

    public void setRequestByteBuf(ByteBuf requestByteBuf) {
        this.requestByteBuf = requestByteBuf;
    }

    private void read(TProtocol iproto, JsonCallback writer) throws TException {
        iproto.readStructBegin();
        writer.onStartObject();

        while (true) {
            TField field = iproto.readFieldBegin();
            if (field.type == TType.STOP) break;

            Field fld = optimizedStruct.get(field.id);

            boolean skip = fld == null;

            if (!skip) {
                writer.onStartField(fld.name);
                readField(iproto, fld.dataType, field.type, writer);
                writer.onEndField();
            } else { // skip reading
                TProtocolUtil.skip(iproto, field.type);
            }

            iproto.readFieldEnd();
        }


        iproto.readStructEnd();
        writer.onEndObject();
    }

    private void readField(TProtocol iproto, DataType fieldDataType, byte fieldType,
                           JsonCallback writer) throws TException {
        switch (fieldType) {
            case TType.VOID:
                break;
            case TType.BOOL:
                boolean boolValue = iproto.readBool();
                writer.onBoolean(boolValue);
                break;
            case TType.BYTE:
                byte b = iproto.readByte();
                writer.onNumber(b);
                break;
            case TType.DOUBLE:
                double dValue = iproto.readDouble();
                writer.onNumber(dValue);
                break;
            case TType.I16:
                short sValue = iproto.readI16();
                writer.onNumber(sValue);
                break;
            case TType.I32:
                int iValue = iproto.readI32();
                if (fieldDataType != null && fieldDataType.kind == DataType.KIND.ENUM) {
                    String enumLabel = findEnumItemLabel(optimizedService.enumMap.get(fieldDataType.qualifiedName), iValue);
                    writer.onString(enumLabel);
                } else {
                    writer.onNumber(iValue);
                }
                break;
            case TType.I64:
                long lValue = iproto.readI64();
                writer.onNumber(lValue);
                break;
            case TType.STRING:
                String strValue = iproto.readString();
                writer.onString(strValue);
                break;
            case TType.STRUCT:
                String subStructName = fieldDataType.qualifiedName;
                OptimizedMetadata.OptimizedStruct subStruct = optimizedService.optimizedStructs.get(subStructName);
                new JsonSerializer(optimizedService, method, version, subStruct).read(iproto, writer);

                break;
            case TType.MAP:
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

                    readField(iproto, fieldDataType.valueType, map.valueType, writer);
                    writer.onEndField();
                }
                writer.onEndObject();
                break;
            case TType.SET:
                TSet set = iproto.readSetBegin();
                writer.onStartArray();
                readCollection(set.size, set.elemType, fieldDataType.valueType, fieldDataType.valueType.valueType, iproto, writer);
                writer.onEndArray();

                break;
            case TType.LIST:
                TList list = iproto.readListBegin();
                writer.onStartArray();
                readCollection(list.size, list.elemType, fieldDataType.valueType, fieldDataType.valueType.valueType, iproto, writer);
                writer.onEndArray();
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
    private void readCollection(int size, byte elemType, DataType metadataType,
                                DataType subMetadataType, TProtocol iproto,
                                JsonCallback writer) throws TException {
        OptimizedMetadata.OptimizedStruct struct = null;
        if (metadataType.kind == DataType.KIND.STRUCT) {
            struct = optimizedService.optimizedStructs.get(metadataType.qualifiedName);
        }
        for (int index = 0; index < size; index++) {
            //没有嵌套结构,也就是原始数据类型, 例如int, boolean,string等
            if (!isComplexKind(metadataType.kind)) {
                readField(iproto, metadataType, elemType, writer);
            } else {
                if (struct != null) {
                    new JsonSerializer(optimizedService, method, version, struct).read(iproto, writer);
                } else if (isCollectionKind(metadataType.kind)) {
                    //处理List<list<>>
                    TList list = iproto.readListBegin();
                    writer.onStartArray();
                    readCollection(list.size, list.elemType, subMetadataType, subMetadataType.valueType, iproto, writer);
                    writer.onEndArray();
                } else if (metadataType.kind == DataType.KIND.MAP) {
                    readField(iproto, metadataType, elemType, writer);
                }
            }
            writer.onEndField();
        }

    }

}
