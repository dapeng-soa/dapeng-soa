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
package com.github.dapeng.core.definition;

import com.github.dapeng.core.*;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.SoaException;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Created by lihuimin on 2017/12/14.
 */
public abstract class SoaFunctionDefinition<I, REQ, RESP> {

    public static abstract class Sync<I, REQ, RESP> extends SoaFunctionDefinition<I, REQ, RESP> {

        public Sync(String methodName, BeanSerializer<REQ> reqSerializer, BeanSerializer<RESP> respSerializer) {
            super(methodName, reqSerializer, respSerializer);
        }

        public abstract RESP apply(I iface, REQ req) throws SoaException;
    }

    public static abstract class Async<I, REQ, RESP> extends SoaFunctionDefinition<I, REQ, RESP> {

        public Async(String methodName, BeanSerializer<REQ> reqSerializer, BeanSerializer<RESP> respSerializer) {
            super(methodName, reqSerializer, respSerializer);
        }

        public abstract Future<RESP> apply(I iface, REQ req) throws SoaException;
    }

    public final String methodName;
    public final BeanSerializer<REQ> reqSerializer;
    public final BeanSerializer<RESP> respSerializer;
    private Optional<CustomConfigInfo> customConfigInfo = Optional.empty();

    public Optional<CustomConfigInfo> getCustomConfigInfo() {
        return customConfigInfo;
    }

    public void setCustomConfigInfo(CustomConfigInfo customConfigInfo) {
        this.customConfigInfo = Optional.of(customConfigInfo);
    }

    public SoaFunctionDefinition(String methodName, BeanSerializer<REQ> reqSerializer, BeanSerializer<RESP> respSerializer) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
    }


}
