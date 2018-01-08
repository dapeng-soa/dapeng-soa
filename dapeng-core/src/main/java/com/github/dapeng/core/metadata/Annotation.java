package com.github.dapeng.core.metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tangliu on 17/7/12.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Annotation {

    public Annotation() {

    }

    public Annotation(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @XmlAttribute
    public String key;

    @XmlAttribute
    public String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
