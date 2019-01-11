/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
