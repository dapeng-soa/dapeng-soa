package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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
            final Random random = new Random();

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

    /**
     * 最小连接数算法：选取在途请求数最小的实例返回，
     * 当存在多个在途请求数最小的实例时，随机选取一个实例返回
     *
     * @param instances
     * @return
     */
    public static RuntimeInstance leastActive(List<RuntimeInstance> instances) {
        RuntimeInstance result = null;
        if (instances.size() > 0) {
            final Random random = new Random();
            int index = 0;
            List<RuntimeInstance> sameLeastCount = new ArrayList<>(16);
            sameLeastCount.add(instances.get(index));
            for (int i = 1; i < instances.size(); i++) {
                if (instances.get(i).getActiveCount() < instances.get(index).getActiveCount()) {
                    index = i;
                    sameLeastCount.clear();
                    sameLeastCount.add(instances.get(index));
                } else if (instances.get(i).getActiveCount() == instances.get(index).getActiveCount()) {
                    sameLeastCount.add(instances.get(i));
                }
            }
            int length = sameLeastCount.size();
            if (length > 1) {
                result = sameLeastCount.get(random.nextInt(length));
            } else {
                result = sameLeastCount.get(0);
            }
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
