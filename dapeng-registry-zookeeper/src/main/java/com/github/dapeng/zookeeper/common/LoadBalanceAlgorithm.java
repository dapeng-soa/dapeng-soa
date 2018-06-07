package com.github.dapeng.zookeeper.common;

import com.github.dapeng.core.RuntimeInstance;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * runtimes 负载均衡策略
 *
 * @author lihuimin
 * @date 2017/12/26
 */
public class LoadBalanceAlgorithm {

    private static int lastIndex = -1;
    private static int currentWeight = 0;

    /**
     * 带权重的随机算法
     * @param instances
     * @return
     */
    public static RuntimeInstance random(List<RuntimeInstance> instances) {
        //随机选择一个可用server
        RuntimeInstance result = null;

        if(instances.size() > 0) {
            int length = instances.size();
            boolean sameWeight = true;
            int totalWeight = 0;
            final  Random random = new Random();
            for (int i = 0; i < length; i++) {
                int tempWeight = instances.get(i).weight;
                totalWeight += tempWeight;
                if (sameWeight && i > 0 && tempWeight != instances.get(i - 1).weight) {
                    sameWeight = false;
                }
            }

            if (totalWeight > 0 && !sameWeight) {
                int offset = random.nextInt(totalWeight);
                for (int i = 0; i < length; i++){
                    offset -= instances.get(i).weight;
                    if (offset < 0){
                        return instances.get(i);
                    }
                }
            }else {
                return instances.get(random.nextInt(length));
            }
        }

        return result;
    }

    /**
     * 最小在途请求
     * @param instances
     * @return
     */
    public static RuntimeInstance leastActive(List<RuntimeInstance> instances) {
        RuntimeInstance result = null;
        if (instances.size() > 0) {

            int index = 0;

            for (int i = 1; i < instances.size(); i++) {
                if (instances.get(i).getActiveCount().intValue() < instances.get(index).getActiveCount().intValue()) {
                    index = i;
                }
            }
            result = instances.get(index);

        }
        return result;
    }

    /**
     * 带权重的轮询算法
     * @param instances
     * @return
     */
    public static RuntimeInstance roundRobin(List<RuntimeInstance> instances) {

        RuntimeInstance result = null;

/*        if (instances.size() > 0) {
            roundRobinIndex = new AtomicInteger(roundRobinIndex.incrementAndGet() % instances.size());
            result = instances.get(roundRobinIndex.get());
        }*/

        if (instances.size() >0){
            int length = instances.size();
            int []weights = new int[length];
            int maxWeight = 0;
            int minweight = Integer.MAX_VALUE;
            for (int i = 0; i < length; i++){
                int tempWeight = instances.get(i).weight;
                maxWeight = Math.max(maxWeight,tempWeight);
                minweight = Math.min(minweight,tempWeight);
                weights[i] = tempWeight;
            }

            //实例权重相同
            if (minweight == maxWeight){
                return  instances.get((++lastIndex) % length);
            }
            //计算权重最大公约数
            int gcdWeight = gcdWeight(weights,weights.length);

            if(lastIndex > length){
                lastIndex = length -1;
            }
            while (true){
                lastIndex = (lastIndex+1) % length;
                if (lastIndex == 0){
                    currentWeight = currentWeight - gcdWeight;
                    if (currentWeight <= 0){
                        currentWeight = maxWeight;
                    }
                }
                if (weights[lastIndex] >= currentWeight){
                    return  instances.get(lastIndex);
                }
            }
        }
        return result;
    }

    /**
     * 计算所有权重的最大公约数
     * @param weights
     * @param lenght
     * @return
     */
    public static int gcdWeight(int[] weights, int lenght){

        if (lenght == 1){
            return weights[0];
        }else {
            return gcd(weights[lenght - 1], gcdWeight(weights, lenght - 1));
        }
    }

    public static int gcd(int a,int b){

        if (b == 0){
            return a;
        }else {
            return gcd(b,a%b);
        }
    }

}
