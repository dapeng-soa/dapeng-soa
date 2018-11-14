package com.github.dapeng.registry.zookeeper;


import com.github.dapeng.router.Route;

import java.util.List;

/**
 * @author lihuimin
 * @date 2017/12/24
 */
public interface ClientZkAgent {

    void start();

    void stop();

    void syncService(String serviceName);

    void cancelSyncService(String serviceName);

    ZkServiceInfo getZkServiceInfo(String serviceName);

    List<Route> getRoutes(String service);

}