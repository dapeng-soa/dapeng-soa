package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.Weight;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import org.apache.zookeeper.Watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class ZkServiceInfo {

    private Watcher watcher = new ZkWatcher(this);

    public enum Status {

        CREATED, ACTIVE, CANCELED,
    }

    final String service;

    private Status status = Status.CREATED;

    /**
     * instances list
     */
    private List<RuntimeInstance> runtimeInstances;

    public ZkServiceInfo(String service) {
        this.service = service;
    }

    public ZkServiceInfo(String service, List<RuntimeInstance> runtimeInstances) {

        this.service = service;
        this.runtimeInstances = runtimeInstances;
    }

    public List<RuntimeInstance> getRuntimeInstances() {
        return runtimeInstances;
    }

    public void setRuntimeInstances(List<RuntimeInstance> runtimeInstances) {
        this.runtimeInstances = runtimeInstances;
    }

    public String getService() {
        return service;
    }

    public Status getStatus() {
        return status;
    }

    public Watcher getWatcher() {
        return watcher;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin  config                              ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    /**
     * processTime zk config
     */
    public Config<Long> processTimeConfig = new Config<>();

    /**
     * timeout zk config
     */
    public Config<Long> timeConfig = new Config<>();
    /**
     * loadBalance zk config
     */
    public Config<LoadBalanceStrategy> loadbalanceConfig = new Config<>();

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
