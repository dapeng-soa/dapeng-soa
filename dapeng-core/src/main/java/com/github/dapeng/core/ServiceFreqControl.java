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
package com.github.dapeng.core;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 描述: 服务限流规则
 *
 * @author Ever
 * @date 2018年08月13日 下午10:28
 */
public class ServiceFreqControl {

    public final String serviceName;
    public final List<FreqControlRule> globalRules;
    public final Map<String, List<FreqControlRule>> rules4methods;

    public ServiceFreqControl(String serviceName, List<FreqControlRule> gobalRule, Map<String, List<FreqControlRule>> rules4methods) {
        this.serviceName = serviceName;
        this.globalRules = gobalRule;
        this.rules4methods = rules4methods;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("service:" + serviceName + ", ");
        sb.append(" globalRules:[").append(globalRules.stream().map(FreqControlRule::toString).collect(Collectors.joining(","))).append("], ");
        sb.append(" rules4methods:[");
        rules4methods.forEach((method, rules) -> {
            sb.append(method).append(":[").append(rules.stream().map(FreqControlRule::toString).collect(Collectors.joining(","))).append("], ");
        });
        sb.append("]");
        return  sb.toString();
    }
}
