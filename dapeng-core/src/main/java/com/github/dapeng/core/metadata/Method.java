package com.github.dapeng.core.metadata;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Method {

    @XmlAttribute
    public String name;
    public String doc;
    public String label;

    public Struct request;
    public Struct response;

    @XmlElementWrapper(name = "annotations")
    @XmlElement(name = "annotation")
    public List<Annotation> annotations;

    public boolean isSoaTransactionProcess;

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

    public Struct getRequest() {
        return request;
    }

    public void setRequest(Struct request) {
        this.request = request;
    }

    public Struct getResponse() {
        return response;
    }

    public void setResponse(Struct response) {
        this.response = response;
    }


    public boolean isSoaTransactionProcess() {
        return isSoaTransactionProcess;
    }

    public void setSoaTransactionProcess(boolean soaTransactionProcess) {
        isSoaTransactionProcess = soaTransactionProcess;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }
}
