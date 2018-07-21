package com.github.dapeng.registry;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.enums.LoadBalanceStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * desc: 注册到注册中心的 服务的信息 --> ZKServiceInfo、EtcdServiceInfo
 *
 * @author hz.lei
 * @since 2018年07月19日 下午3:57
 */
public class RegisterInfo {
    public enum Status {

        CREATED, ACTIVE, CANCELED,
    }

    public final String service;

    private Status status = Status.CREATED;

    /**
     * instances list
     */
    private List<RuntimeInstance> runtimeInstances;

    public RegisterInfo(String service) {
        this.service = service;
    }

    public RegisterInfo(String service, List<RuntimeInstance> runtimeInstances) {

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

    public void setStatus(Status status) {
        this.status = status;
    }

    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin  config                              ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    /**
     * timeout zk config
     */
    public Config<Long> timeConfig = new Config<>();
    /**
     * loadBalance zk config
     */
    public Config<LoadBalanceStrategy> loadbalanceConfig = new Config<>();

    /**
     * config class
     */
    public static class Config<T> {
        public T globalConfig;
        public Map<String, T> serviceConfigs = new HashMap<>();
        public Map<String, T> instanceConfigs = new HashMap<>();
    }
}
