package com.github.dapeng.registry;

import java.util.List;
import java.util.Properties;

/**
 * Created by lihuimin on 2017/12/25.
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
