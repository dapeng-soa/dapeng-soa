package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.RuntimeInstance;

import java.util.List;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class ZkServiceInfo {

    final String service;

    /**
     * 是否需要对zk上该服务节点的监听
     */
    boolean isCancel;

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

}
