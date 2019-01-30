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
package com.github.dapeng.impl.filters.freq;

/**
 * 描述:  计数节点
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:49
 */
public class CounterNode {
    /**
     * app 被映射为 16bit id
     */
    public final short appId;
    /**
     * rule_type_id 被映射为 16bit id
     */
    public final short ruleTypeId;
    /**
     * ip, userId, callerMid etc.
     */
    public final int key;
    /**
     * last updated unix epoch, seconds since 1970.
     */
    public final int timestamp;
    /**
     * min interval counter
     */
    public final int minCount;
    /**
     * mid interval counter
     */
    public final int midCount;
    /**
     * max interval counter
     */
    public final int maxCount;

    public CounterNode(short appId, short ruleTypeId, int key,
                       int timestamp, int minCount, int midCount,
                       int maxCount) {
        this.appId = appId;
        this.ruleTypeId = ruleTypeId;
        this.key = key;
        this.timestamp = timestamp;
        this.minCount = minCount;
        this.midCount = midCount;
        this.maxCount = maxCount;
    }

    @Override
    public String toString() {
        return "appId:" + appId + ", ruleTypeId:" + ruleTypeId + ", key:" + key
                + ", timestamp:" + timestamp + ", counters:" + minCount + "/" + midCount + "/" + maxCount;
    }
}
