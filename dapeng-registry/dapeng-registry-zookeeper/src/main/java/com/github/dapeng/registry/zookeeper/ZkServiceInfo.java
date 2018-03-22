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
public class ZkServiceInfo {

    final String service;
    /**
     * instances list
     */
    private List<RuntimeInstance> runtimeInstances;
    /**
     * zk service configInfo
     */
    private ZkConfigInfo configInfo;

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

    public ZkConfigInfo getConfigInfo() {
        return configInfo;
    }

    public void setConfigInfo(ZkConfigInfo configInfo) {
        this.configInfo = configInfo;
    }
}
