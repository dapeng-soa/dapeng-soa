package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.ConfigKey;

import java.util.Map;

/**
 * @author lihuimin
 * @date 2017/12/24
 */
public interface ZkClientAgent {

    void start();

    void stop();

    void syncService(String serviceName, Map<String, ZkServiceInfo> zkInfos);

    void cancnelSyncService(String serviceName, Map<String, ZkServiceInfo> zkInfos);

    Map<ConfigKey,Object> getConfig(boolean usingFallback, String serviceKey);

}