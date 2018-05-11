package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author lihuimin
 * @date 2017/12/26
 */
public class LoadBalanceAlgorithm {

    private static AtomicInteger roundRobinIndex = new AtomicInteger(8);


    public static RuntimeInstance random(List<RuntimeInstance> instances) {
        //随机选择一个可用server
        RuntimeInstance result = null;
        if (instances.size() > 0) {
            final Random random = new Random();

            final int index = random.nextInt(instances.size());

            result = instances.get(index);
        }
        return result;
    }

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

    public static RuntimeInstance roundRobin(List<RuntimeInstance> instances) {

        RuntimeInstance result = null;

        if (instances.size() > 0) {
            roundRobinIndex = new AtomicInteger(roundRobinIndex.incrementAndGet() % instances.size());
            result = instances.get(roundRobinIndex.get());
        }
        return result;
    }


}
