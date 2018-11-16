package com.github.dapeng.registry.zookeeper2;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.Weight;
import com.github.dapeng.core.enums.LoadBalanceStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * service information of ZK, including runtime and config
 *
 * @author ever
 * @date 2018/11/16
 */
public class ZkServiceInfo {
    private final String ServiceName;

    /**
     * instances list, always use cow collection
     */
    private List<RuntimeInstance> runtimeInstances;

    public ZkServiceInfo(String serviceName, List<RuntimeInstance> runtimeInstances) {
        this.ServiceName = serviceName;
        this.runtimeInstances = runtimeInstances;
    }

    public List<RuntimeInstance> getRuntimeInstances() {
        return runtimeInstances;
    }

    public String serviceName() {
        return ServiceName;
    }

    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin  config                              ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    /**
     * processTime zk config
     */
    public com.github.dapeng.registry.zookeeper.ZkServiceInfo.Config<Long> processTimeConfig = new com.github.dapeng.registry.zookeeper.ZkServiceInfo.Config<>();

    /**
     * timeout zk config
     */
    public com.github.dapeng.registry.zookeeper.ZkServiceInfo.Config<Long> timeConfig = new com.github.dapeng.registry.zookeeper.ZkServiceInfo.Config<>();
    /**
     * loadBalance zk config
     */
    public com.github.dapeng.registry.zookeeper.ZkServiceInfo.Config<LoadBalanceStrategy> loadbalanceConfig = new com.github.dapeng.registry.zookeeper.ZkServiceInfo.Config<>();

    /**
     * weight zk config
     */
    public Weight weightGlobalConfig = new Weight();

    public List<Weight> weightServiceConfigs = new ArrayList<>();

    /**
     * config class
     */
    public static class Config<T> {
        public T globalConfig;
        public Map<String, T> serviceConfigs = new HashMap<>();
        public Map<String, T> instanceConfigs = new HashMap<>();
    }
}
