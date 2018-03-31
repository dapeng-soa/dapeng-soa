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

    void syncService(ZkServiceInfo zkInfo);

    void cancnelSyncService(ZkServiceInfo zkInfo);

    ZkConfigInfo getConfig(boolean usingFallback, String serviceKey);

}