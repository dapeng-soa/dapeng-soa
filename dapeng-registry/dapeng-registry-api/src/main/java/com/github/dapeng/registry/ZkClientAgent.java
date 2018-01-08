package com.github.dapeng.registry;

import java.util.Map;

/**
 * Created by lihuimin on 2017/12/24.
 */
public interface ZkClientAgent {

    void start();

    void stop();

    void syncService(String serviceName, Map<String,ServiceZKInfo> zkInfos);

    void cancnelSyncService(String service);

    Map<ConfigKey,Object> getConfig(boolean usingFallback, String serviceKey);



}