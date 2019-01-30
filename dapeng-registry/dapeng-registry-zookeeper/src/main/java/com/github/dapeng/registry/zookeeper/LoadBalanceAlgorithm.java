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
package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lihuimin
 * @date 2017/12/26
 */
public class LoadBalanceAlgorithm {
    private static int lastIndex = -1;
    private static int currentWeight = 0;


    /**
     * 带权重的随机算法
     *
     * @param instances
     * @return
     */
    public static RuntimeInstance random(List<RuntimeInstance> instances) {
        //随机选择一个可用server
        RuntimeInstance result = null;

        if (instances.size() > 0) {
            int length = instances.size();
            final ThreadLocalRandom random = ThreadLocalRandom.current();

            int totalWeight = 0;
            int minWeight = Integer.MAX_VALUE;
            int maxWeight = 0;
            for (int i = 0; i < length; i++) {
                int tempWeight = instances.get(i).weight;
                totalWeight += tempWeight;
                maxWeight = Math.max(maxWeight, tempWeight);
                minWeight = Math.min(minWeight, tempWeight);
            }
            boolean isSame = (minWeight == maxWeight);

            if (totalWeight > 0 && !isSame) {
                int offset = random.nextInt(totalWeight);
                for (int i = 0; i < length; i++) {
                    offset -= instances.get(i).weight;
                    if (offset < 0) {
                        return instances.get(i);
                    }
                }
            } else {
                return instances.get(random.nextInt(length));
            }
        }

        return result;
    }

    public static RuntimeInstance leastActive(List<RuntimeInstance> instances) {
        RuntimeInstance result = null;
        if (instances.size() > 0) {
            int index = 0;
            for (int i = 1; i < instances.size(); i++) {
                if (instances.get(i).getActiveCount() < instances.get(index).getActiveCount()) {
                    index = i;
                }
            }
            result = instances.get(index);
        }
        return result;
    }

    /**
     * 带权重的轮询算法
     *
     * @param instances
     * @return
     */
    public static RuntimeInstance roundRobin(List<RuntimeInstance> instances) {

        RuntimeInstance result = null;

        if (instances.size() > 0) {
            int length = instances.size();
            int[] weights = new int[length];
            int maxWeight = 0;
            int minWeight = Integer.MAX_VALUE;
            for (int i = 0; i < length; i++) {
                int tempWeight = instances.get(i).weight;
                maxWeight = Math.max(maxWeight, tempWeight);
                minWeight = Math.min(minWeight, tempWeight);
                weights[i] = tempWeight;
            }
            boolean isSame = (minWeight == maxWeight);
            //计算权重最大公约数
            int gcdWeight = gcdWeight(weights, weights.length);

            //实例权重相同
            if (isSame) {
                return instances.get((++lastIndex) % length);
            }


            if (lastIndex >= length) {
                lastIndex = length - 1;
            }
            while (true) {
                lastIndex = (lastIndex + 1) % length;
                if (lastIndex == 0) {
                    currentWeight = currentWeight - gcdWeight;
                    if (currentWeight <= 0) {
                        currentWeight = maxWeight;
                    }
                }
                if (weights[lastIndex] >= currentWeight) {
                    return instances.get(lastIndex);
                }
            }
        }
        return result;
    }

    /**
     * 计算所有权重的最大公约数
     *
     * @param weights
     * @param lenght
     * @return
     */
    public static int gcdWeight(int[] weights, int lenght) {

        if (lenght == 1) {
            return weights[0];
        } else {
            return gcd(weights[lenght - 1], gcdWeight(weights, lenght - 1));
        }
    }

    public static int gcd(int a, int b) {

        if (b == 0) {
            return a;
        } else {
            return gcd(b, a % b);
        }
    }
}
