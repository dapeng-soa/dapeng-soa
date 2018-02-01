package com.github.dapeng.core.metadata;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Struct {

    @XmlAttribute
    public String namespace;
    @XmlAttribute
    public String name;

    public String label;
    public String doc;

    @XmlElementWrapper(name = "fields")
    @XmlElement(name = "field")
    public List<Field> fields;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }
}
