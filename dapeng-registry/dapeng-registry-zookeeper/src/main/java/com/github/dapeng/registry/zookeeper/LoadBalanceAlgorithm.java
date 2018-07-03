package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author lihuimin
 * @date 2017/12/26
 */
public class LoadBalanceAlgorithm {
    private static int lastIndex = -1;
    private static int currentWeight = 0;
    private static boolean isSame = true;
    private static int gcdWeight = 1;
    private static int maxWeight = 0;
    private static int totalWeight = 0;
    private static int[] weights;


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
            final Random random = new Random();
            if (SoaSystemEnvProperties.SOA_CHANGE_WEIGHE) {
                totalWeight = 0;
                int minWeight = Integer.MAX_VALUE;
                for (int i = 0; i < length; i++) {
                    int tempWeight = instances.get(i).weight;
                    totalWeight += tempWeight;
                    maxWeight = Math.max(maxWeight, tempWeight);
                    minWeight = Math.min(minWeight, tempWeight);
                }
                isSame = (minWeight == maxWeight);
                SoaSystemEnvProperties.SOA_CHANGE_WEIGHE = false;
            }
            if (totalWeight > 0 && !isSame) {
                int offset = random.nextInt(totalWeight);
                for (int i = 0; i < length; i++){
                    offset -= instances.get(i).weight;
                    if (offset < 0){
                        return instances.get(i);
                    }
                }
            }else {
         //       result = instances.get(random.nextInt(length));
                return instances.get(random.nextInt(length));
            }
        }

        return result;
    }

    public static RuntimeInstance leastActive(List<RuntimeInstance> instances) {
        RuntimeInstance result = null;
        if (instances.size() > 0) {

            SoaSystemEnvProperties.SOA_CHANGE_WEIGHE = false;

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

        if (instances.size() >0){

            int length = instances.size();
            if (SoaSystemEnvProperties.SOA_CHANGE_WEIGHE) {
                weights = new int[length];
                maxWeight = 0;
                int minWeight = Integer.MAX_VALUE;
                for (int i = 0; i < length; i++) {
                    int tempWeight = instances.get(i).weight;
                    maxWeight = Math.max(maxWeight, tempWeight);
                    minWeight = Math.min(minWeight, tempWeight);
                    weights[i] = tempWeight;
                }
                isSame = (minWeight == maxWeight);
                //计算权重最大公约数
                gcdWeight = gcdWeight(weights,weights.length);
                SoaSystemEnvProperties.SOA_CHANGE_WEIGHE = false;
            }

            //实例权重相同
            if (isSame){
                return  instances.get((++lastIndex) % length);
            }


            if(lastIndex >= length){
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
