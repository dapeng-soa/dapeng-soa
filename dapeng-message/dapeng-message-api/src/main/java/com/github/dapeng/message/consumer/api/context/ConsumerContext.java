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
package com.github.dapeng.message.consumer.api.context;

import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaFunctionDefinition;


/**
 * Created by tangliu on 2016/8/4.
 */
public class ConsumerContext {

    public Object iface;

    public SoaFunctionDefinition<Object, Object, Object> soaFunctionDefinition;

    Object action;
    Object consumer;
    String groupId;
    String topic;
    /**
     * event Type
     */
    String eventType;


    public Object getIface() {
        return iface;
    }

    public void setIface(Object iface) {
        this.iface = iface;
    }

    public SoaFunctionDefinition<Object, Object, Object> getSoaFunctionDefinition() {
        return soaFunctionDefinition;
    }

    public void setSoaFunctionDefinition(SoaFunctionDefinition<Object, Object, Object> soaFunctionDefinition) {
        this.soaFunctionDefinition = soaFunctionDefinition;
    }

    public Object getAction() {
        return action;
    }

    public void setAction(Object action) {
        this.action = action;
    }

    public Object getConsumer() {
        return consumer;
    }

    public void setConsumer(Object consumer) {
        this.consumer = consumer;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
