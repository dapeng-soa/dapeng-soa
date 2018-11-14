package com.github.dapeng.registry.zookeeper;


import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author lihuimin
 * @date 2017/12/24
 */
public class ClientZkAgentImpl implements ClientZkAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientZkAgentImpl.class);
    /**
     * 是否使用 灰度 zk
     */
    private final boolean usingFallbackZk = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG;

    private ClientZk masterZk, fallbackZk;

    public ClientZkAgentImpl() {
        start();
    }

    @Override
    public void start() {
        masterZk = ClientZk.getMasterInstance();
        // fallback
        if (usingFallbackZk) {
            fallbackZk = ClientZk.getFallbackInstance();
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

    @Override
    public void cancelSyncService(String serviceName) {
        LOGGER.info("cancelSyncService:[" + serviceName + "]");
        masterZk.cancelSyncService(serviceName);
    }

    @Override
    public void syncService(String serviceName) {
        masterZk.syncServiceZkInfo(serviceName);
        //TODO fallbackZk?
    }

    @Override
    public List<Route> getRoutes(String service) {
        return masterZk.getRoutes(service);
    }

    @Override
    public ZkServiceInfo getZkServiceInfo(String service) {
        return masterZk.getZkServiceInfo(service);
    }
}
