package com.github.dapeng.zookeeper.agent.impl;

import com.github.dapeng.zookeeper.common.ZkServiceInfo;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.zookeeper.agent.ClientZkAgent;
import com.github.dapeng.zookeeper.client.ClientZk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 客户端 zk agent 实现
 *
 * @author huyjO
 * @Created 2018/5/24 22:21
 */
public class ClientZkAgentImpl implements ClientZkAgent {

    private static final Logger logger = LoggerFactory.getLogger(ClientZkAgentImpl.class);
    private static final ClientZkAgent clientZkAgentInstance = new ClientZkAgentImpl();
    /**
     * 是否使用 灰度 zk
     */
    private final boolean usingFallbackZk = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG;
    private static ClientZk masterZk, fallbackZk;

    public static ClientZkAgent getClientZkAgentInstance() {
        return clientZkAgentInstance;
    }

    private ClientZkAgentImpl() {
        init();
    }

    @Override
    public void init() {
        masterZk = new ClientZk(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        if (usingFallbackZk) {
            fallbackZk = new ClientZk(SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_HOST);
        }
    }

    @Override
    public void stop() {
        if (masterZk != null) {
            masterZk.destroy();
        }
        if (fallbackZk != null) {
            fallbackZk.destroy();
        }
    }


    public ClientZk getZkClient() {
        return masterZk;
    }

    @Override
    public void syncService(ZkServiceInfo zkServiceInfo) {
        List<RuntimeInstance> runtimeInstances = usingFallbackZk ? fallbackZk.getZkDataContext().getRuntimeInstancesMap().get(zkServiceInfo.getServiceName()) : masterZk.getZkDataContext().getRuntimeInstancesMap().get(zkServiceInfo.getServiceName());
        if (runtimeInstances != null && !runtimeInstances.isEmpty()) {
            zkServiceInfo.setStatus(ZkServiceInfo.Status.ACTIVE);
            logger.error(getClass().getSimpleName() + "::syncService [service: " + zkServiceInfo.getServiceName() + "] ,runtimeInstances :[" + runtimeInstances + "] ");
        } else {//没有实例  就要 注销服务
            zkServiceInfo.setStatus(ZkServiceInfo.Status.CANCELED);
            logger.error(getClass().getSimpleName() + "::syncService [service: " + zkServiceInfo.getServiceName() + "], not found  runtimeInstances.");
        }
    }

    @Override
    public void cancelService(ZkServiceInfo zkServiceInfo) {
        //fixme should remove the debug log
        logger.info("cancelService:[" + zkServiceInfo.getServiceName() + "]");
        zkServiceInfo.setStatus(ZkServiceInfo.Status.CANCELED);
    }

    @Override
    public void activeCountIncrement(RuntimeInstance runtimeInstance) {
        String serviceName = runtimeInstance.service;
        //修改 zkDataContext RuntimeInstance
        List<RuntimeInstance> runtimeInstanceList = usingFallbackZk ? fallbackZk.getZkDataContext().getRuntimeInstancesMap().get(serviceName) : masterZk.getZkDataContext().getRuntimeInstancesMap().get(serviceName);
        for (RuntimeInstance instance : runtimeInstanceList) {
            if (instance.getInstanceInfo().equalsIgnoreCase(runtimeInstance.getInstanceInfo())) {
                instance.increaseActiveCount();
            }
        }
        if (usingFallbackZk) {
            fallbackZk.getZkDataContext().getRuntimeInstancesMap().put(serviceName, runtimeInstanceList);
        } else {
            masterZk.getZkDataContext().getRuntimeInstancesMap().put(serviceName, runtimeInstanceList);
        }

        //修改 zkDataContext ServiceMap
        String serviceInfo = serviceName + ":" + runtimeInstance.version + "[" + runtimeInstance.ip + ":" + runtimeInstance.port + "]";
        List<ZkServiceInfo> zkServiceInfoList = usingFallbackZk ? fallbackZk.getZkDataContext().getServicesMap().get(serviceName) : masterZk.getZkDataContext().getServicesMap().get(serviceName);
        for (ZkServiceInfo info : zkServiceInfoList) {
            if (info.getZkServiceInfo().equalsIgnoreCase(serviceInfo)) {
                info.increaseActiveCount();
            }
        }
        if (usingFallbackZk) {
            fallbackZk.getZkDataContext().getServicesMap().put(serviceName, zkServiceInfoList);
        } else {
            masterZk.getZkDataContext().getServicesMap().put(serviceName, zkServiceInfoList);
        }
    }

    @Override
    public void activeCountDecrement(RuntimeInstance runtimeInstance) {
        String serviceName = runtimeInstance.service;
        //修改 zkDataContext RuntimeInstance
        List<RuntimeInstance> runtimeInstanceList = usingFallbackZk ? fallbackZk.getZkDataContext().getRuntimeInstancesMap().get(serviceName) : masterZk.getZkDataContext().getRuntimeInstancesMap().get(serviceName);
        for (RuntimeInstance instance : runtimeInstanceList) {
            if (instance.getInstanceInfo().equalsIgnoreCase(runtimeInstance.getInstanceInfo())) {
                instance.decreaseActiveCount();
            }
        }
        if (usingFallbackZk) {
            fallbackZk.getZkDataContext().getRuntimeInstancesMap().put(serviceName, runtimeInstanceList);
        } else {
            masterZk.getZkDataContext().getRuntimeInstancesMap().put(serviceName, runtimeInstanceList);
        }

        //修改 zkDataContext ServiceMap
        String serviceInfo = serviceName + ":" + runtimeInstance.version + "[" + runtimeInstance.ip + ":" + runtimeInstance.port + "]";
        List<ZkServiceInfo> zkServiceInfoList = usingFallbackZk ? fallbackZk.getZkDataContext().getServicesMap().get(serviceName) : masterZk.getZkDataContext().getServicesMap().get(serviceName);
        for (ZkServiceInfo info : zkServiceInfoList) {
            if (info.getZkServiceInfo().equalsIgnoreCase(serviceInfo)) {
                info.decreaseActiveCount();
            }
        }
        if (usingFallbackZk) {
            fallbackZk.getZkDataContext().getServicesMap().put(serviceName, zkServiceInfoList);
        } else {
            masterZk.getZkDataContext().getServicesMap().put(serviceName, zkServiceInfoList);
        }
    }

}
