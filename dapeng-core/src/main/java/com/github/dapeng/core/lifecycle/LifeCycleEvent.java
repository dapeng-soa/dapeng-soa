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
package com.github.dapeng.core.lifecycle;

/**
 * 四种状态：start、pause、masterChange、stop
 *
 * @author hui
 * @date 2018/7/27 0027 9:39
 */
public class LifeCycleEvent {
    public enum LifeCycleEventEnum {
        /**
         * dapeng 容器启动
         */
        START,
        PAUSE,
        MASTER_CHANGE,
        CONFIG_CHANGE,
        STOP
    }

    /**
     * 事件类型
     */
    private final LifeCycleEventEnum eventEnum;
    /**
     * service name
     */
    private String service;
    /**
     * 事件发生时附带的一些属性
     */
    private Object attachment;

    public LifeCycleEvent(final LifeCycleEventEnum eventEnum,
                          final String service,
                          final Object attachment) {
        this.eventEnum = eventEnum;
        this.service = service;
        this.attachment = attachment;
    }

    public LifeCycleEvent(LifeCycleEventEnum eventEnum, Object attachment) {
        this.eventEnum = eventEnum;
        this.attachment = attachment;
    }

    public LifeCycleEvent(LifeCycleEventEnum eventEnum) {
        this.eventEnum = eventEnum;
    }

    public Object getAttachment() {
        return attachment;
    }

    public LifeCycleEventEnum getEventEnum() {
        return eventEnum;
    }

    public String getService() {
        return service;
    }
}
