package com.github.dapeng.core.metadata;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TEnum {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EnumItem {
        public String label;
        public int value;
        public String doc;

        @XmlElementWrapper(name = "annotations")
        @XmlElement(name = "annotation")
        public List<Annotation> annotations;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getDoc() {
            return doc;
        }

        public void setDoc(String doc) {
            this.doc = doc;
        }

        public List<Annotation> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(List<Annotation> annotations) {
            this.annotations = annotations;
        }
    }

    @XmlAttribute
    public String namespace;
    @XmlAttribute
    public String name;

    public String doc;
    public String label;

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    public List<EnumItem> enumItems;

    @XmlElementWrapper(name = "annotations")
    @XmlElement(name = "annotation")
    public List<Annotation> annotations;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<EnumItem> getEnumItems() {
        return enumItems;
    }

    public void setEnumItems(List<EnumItem> enumItems) {
        this.enumItems = enumItems;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }
}
