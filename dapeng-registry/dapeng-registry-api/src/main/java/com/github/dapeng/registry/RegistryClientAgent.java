package com.github.dapeng.registry;

import java.util.List;

/**
 * 注册中心 客户端 agent
 *
 * @author lihuimin
 * @date 2017/12/24
 */
public interface RegistryClientAgent {
    /**
     * 启动
     */
    void start();

    /**
     * 停止
     */
    void stop();

    /**
     * 同步注册中心服务列表信息
     *
     * @param registerInfo
     */
    void syncService(RegisterInfo registerInfo);

    /**
     * 不再同步注册中心服务列表信息
     *
     * @param registerInfo
     */
    void cancelSyncService(RegisterInfo registerInfo);

    /**
     * 根据服务名获取注册中心配置的 router信息
     *
     * @param service
     * @param <T>
     * @return
     */
    <T> List<T> getRoutes(String service);

}