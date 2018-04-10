package com.github.dapeng.registry.zookeeper;


/**
 * @author lihuimin
 * @date 2017/12/24
 */
public interface ZkClientAgent {

    void start();

    void stop();

    void syncService(ZkServiceInfo zkInfo);

    void cancelSyncService(ZkServiceInfo zkInfo);


}