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
package com.github.dapeng.core.enums;

/**
 * Load Balance Stratage strategy
 *
 * @author craneding
 * @date 16/1/20
 */
public enum LoadBalanceStrategy {

    /**
     * <ul>
     * <li>随机，按权重设置随机概率。</li>
     * <li>在一个截面上碰撞的概率高，但调用量越大分布越均匀，而且按概率使用权重后也比较均匀，有利于动态调整提供者权重。</li>
     * </ul>
     */
    Random,

    /**
     * <ul>
     * <li>轮循，按公约后的权重设置轮循比率。</li>
     * <li>存在慢的提供者累积请求问题，比如：第二台机器很慢，但没挂，当请求调到第二台时就卡在那，久而久之，所有请求都卡在调到第二台上。</li>
     * </ul>
     */
    RoundRobin,

    /**
     * <ul>
     * <li>最少活跃调用数，相同活跃数的随机，活跃数指调用前后计数差。</li>
     * <li>使慢的提供者收到更少请求，因为越慢的提供者的调用前后计数差会越大。</li>
     * </ul>
     */
    LeastActive,

    /**
     * <ul>
     * <li>一致性Hash，相同参数的请求总是发到同一提供者。</li>
     * <li>当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动。</li>
     * </ul>
     */
    ConsistentHash;

    public static LoadBalanceStrategy findByValue(String value) {
        switch (value) {
            case "random":
                return Random;
            case "roundRobin":
                return RoundRobin;
            case "leastActive":
                return LeastActive;
            case "consistentHash":
                return ConsistentHash;
            default:
                return Random;
        }
    }

}
