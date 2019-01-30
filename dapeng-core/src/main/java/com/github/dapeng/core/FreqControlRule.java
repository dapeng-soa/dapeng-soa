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

import java.util.Set;

/**
 * 描述: 限流规则
 *
 * @author hz.lei
 * @date 2018年05月09日 下午10:03
 */
public class FreqControlRule {

    public String app;
    public String ruleType;
    public Set<Integer> targets;
    public int minInterval;
    public int maxReqForMinInterval;
    public int midInterval;
    public int maxReqForMidInterval;
    public int maxInterval;
    public int maxReqForMaxInterval;

    @Override
    public String toString() {
        return "app:" + app + ", ruleType:" + ruleType + ", targets:" + targets +", freqRule:["
                + minInterval + "," + maxReqForMinInterval + "/"
                + midInterval + "," + maxReqForMidInterval + "/"
                + maxInterval + "," + maxReqForMaxInterval + ";";
    }
}
