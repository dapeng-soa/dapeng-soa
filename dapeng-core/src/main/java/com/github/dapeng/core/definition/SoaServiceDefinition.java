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

import com.github.dapeng.core.CustomConfigInfo;

import java.util.Map;
import java.util.Optional;

/**
 * @author lihuimin
 * @date 2017/12/14
 */
public class SoaServiceDefinition<I> {
    public final I iface;    // sync interface
    public final Class<I> ifaceClass;

    public final Map<String, SoaFunctionDefinition<I, ?, ?>> functions;

    public final boolean isAsync;

    private Optional<CustomConfigInfo> configInfo = Optional.empty();

    public Optional<CustomConfigInfo> getConfigInfo() {

        return configInfo;
    }

    public void setConfigInfo(CustomConfigInfo configInfo) {
        this.configInfo = Optional.of(configInfo);
    }

    public SoaServiceDefinition(I iface, Class<I> ifaceClass,
                                Map<String, SoaFunctionDefinition<I, ?, ?>> functions) {
        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.isAsync = AsyncService.class.isAssignableFrom(iface.getClass());
        this.functions = functions;

        // TODO assert functions.forall( _.isInstance[ SoaFunctionDefinition.Async] )
    }

    public Map<String, SoaFunctionDefinition<I, ?, ?>> buildMap(SoaFunctionDefinition... functions) {
        return null;
    }


}
