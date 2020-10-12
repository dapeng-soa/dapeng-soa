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
package com.github.dapeng.doc;


import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author yuand
 * @date 2018/3/21
 */
public class ServiceInvocationProxy implements InvocationContextImpl.InvocationContextProxy {

    public void init() {
        InvocationContextImpl.Factory.setInvocationContextProxy(this);
    }

    public void destroy() {
    }

    @Override
    public Optional<String> callerMid() {
        return Optional.of("documentSite");
    }

    @Override
    public Map<String, String> cookies() {
        return new HashMap<>(16);
    }

    @Override
    public Optional<Integer> customerId() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> operatorId() {
        return Optional.empty();
    }

    @Override
    public Optional<Long> sessionTid() {
        return Optional.of(DapengUtil.generateTid());
    }

    @Override
    public Optional<Integer> userIp() {
        return Optional.ofNullable(IPUtils.localIpAsInt());
    }
}

