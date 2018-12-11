package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
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
     * 一致性哈希算法
     *
     * @param instances
     * @return
     */
    public static RuntimeInstance consistentHash(List<RuntimeInstance> instances) {
        RuntimeInstance result = null;
        if (instances.size() > 0) {
            int multiple = 160;
            SortedMap<Integer, RuntimeInstance> virtualHashCircle = new TreeMap<>();
            createCircle(instances, virtualHashCircle, multiple);
            String request = InvocationContextImpl.Factory.currentInstance().cookie("cookie_hash");
            if (request == null) {
                result = random(instances);
            } else {
                result = getServer(request, virtualHashCircle);
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

    private static void createCircle(List<RuntimeInstance> instances, SortedMap<Integer, RuntimeInstance> virtualHashCircle, int multiple) {
        for (RuntimeInstance ins : instances) {
            for (int i = 0; i < multiple; i++) {
                String virtualServer = ins.ip + ":" + ins.port + "#" + i;
                int virtualHash = getHash(virtualServer);
                virtualHashCircle.put(virtualHash, ins);
            }
        }
    }

    /**
     * FNV1_32_HASH算法
     *
     * @param
     * @return
     */
    private static int getHash(String str) {
        String hashParameter = null;
        if (str.startsWith("&")) {
            switch (str.substring(1)) {
                case "methodName":
                    hashParameter = InvocationContextImpl.Factory.currentInstance().methodName();
                    break;
                case "callerIp":
                    hashParameter = InvocationContextImpl.Factory.currentInstance().callerIp().map(String::valueOf).orElse("");
                    break;
                case "userIp":
                    hashParameter = InvocationContextImpl.Factory.currentInstance().userIp().map(String::valueOf).orElse("");
                default:
                    // won't be here
            }
        } else {
            hashParameter = str;
        }
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < hashParameter.length(); i++) {
            hash = (hash ^ hashParameter.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        // 如果算出来的值为负数则取其绝对值
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        return hash;
    }

    private static RuntimeInstance getServer(String request, SortedMap<Integer, RuntimeInstance> virtualHashCircle) {
        int hash = getHash(request);
        SortedMap<Integer, RuntimeInstance> subMap = virtualHashCircle.tailMap(hash);
        if (subMap.isEmpty()) {
            return virtualHashCircle.get(virtualHashCircle.firstKey());
        }
        Integer target = subMap.firstKey();
        return subMap.get(target);
    }
}
