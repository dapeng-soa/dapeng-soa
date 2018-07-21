package com.github.dapeng.registry;

import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;

import java.util.Map;

/**
 * desc: 服务端 注册到注册中心 agent
 *
 * @author hz.lei
 * @since 2018年07月19日 下午4:09
 */
public interface RegistryServerAgent {

    /**
     * start
     */
    void start();

    /**
     * stop
     */
    void stop();

    /**
     * 注册服务
     *
     * @param serverName  服务名
     * @param versionName 版本号
     */
    void registerService(String serverName, String versionName);

    /**
     * 卸载服务
     *
     * @param serverName
     * @param versionName
     */
    void unregisterService(String serverName, String versionName);

    /**
     * 注册服务集合
     */
    void registerAllServices();

    /**
     * 设置处理器集合
     *
     * @param processorMap 处理器集合
     */
    void setProcessorMap(Map<ProcessorKey, SoaServiceDefinition<?>> processorMap);

    /**
     * 获取处理器集合
     */
    Map<ProcessorKey, SoaServiceDefinition<?>> getProcessorMap();


    /**
     * 获取配置
     *
     * @param usingFallback
     * @param serviceKey
     * @return
     */
    RegisterInfo getConfig(boolean usingFallback, String serviceKey);
}
