package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.RuntimeInstance;

import java.util.List;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class ZkServiceInfo {

    public enum Status {

        CREATED,ACTIVE,CANCELED,
    }
    final String service;

    private Status status = Status.CREATED;

    /**
     * instances list
     */
    private List<RuntimeInstance> runtimeInstances;

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

    public void setStatus(Status status) {
        this.status = status;
    }
}
