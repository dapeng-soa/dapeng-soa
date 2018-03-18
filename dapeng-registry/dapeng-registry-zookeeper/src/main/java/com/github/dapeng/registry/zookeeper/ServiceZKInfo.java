package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.LoadBalanceStrategy;
import com.github.dapeng.registry.RuntimeInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class ServiceZKInfo {

    final String service;

    private List<RuntimeInstance> runtimeInstances;
    // timeout
    public Config<Long> timeConfig = new Config<>();
    //loadbalance config
    public Config<LoadBalanceStrategy> loadbalanceConfig = new Config<>();

    public ServiceZKInfo(String service, List<RuntimeInstance> runtimeInstances) {

        this.service = service;
        this.runtimeInstances = runtimeInstances;
    }

    public List<RuntimeInstance> getRuntimeInstances() {
        return runtimeInstances;
    }

    public void setRuntimeInstances(List<RuntimeInstance> runtimeInstances) {
        this.runtimeInstances = runtimeInstances;
    }

    public static class Config<T> {
        public T globalConfig;
        public Map<String, T> serviceConfigs = new HashMap<>();
        public Map<String, T> instanceConfigs = new HashMap<>();
    }

}
