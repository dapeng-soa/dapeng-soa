package com.github.dapeng.core.metadata;

import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DataType {

    @XmlEnum
    public static enum KIND {
        VOID, BOOLEAN, BYTE, SHORT, INTEGER, LONG, DOUBLE, STRING, BINARY, MAP, LIST, SET, ENUM, STRUCT, DATE, BIGDECIMAL;
    }

    public KIND kind;
    public DataType keyType;
    public DataType valueType;
    /**
     * for STRUCT/ENUM, use qualfiedName and service.structDefinitions/enumDefinitions
     * together to resolve the type
     */
    @XmlElement(name = "ref")
    public String qualifiedName;

    public KIND getKind() {
        return kind;
    }

    public void setKind(KIND kind) {
        this.kind = kind;
    }

    public DataType getKeyType() {
        return keyType;
    }

    public void setKeyType(DataType keyType) {
        this.keyType = keyType;
    }

    public DataType getValueType() {
        return valueType;
    }

    public void setValueType(DataType valueType) {
        this.valueType = valueType;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

}
