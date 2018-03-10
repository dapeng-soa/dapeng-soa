package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.RuntimeInstance;

import java.util.List;
import java.util.Properties;

/**
 *
 * @author lihuimin
 * @date 2017/12/25
 */
public class ServiceZKInfo {

    final String service;

    private List<RuntimeInstance> runtimeInstances;
    Properties config;

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
}
