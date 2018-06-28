package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;

import java.util.List;
import java.util.Map;

/**
 * Registry Agent
 *
 * @author craneding
 * @date 16/3/1
 */
public interface ServerZkAgent {

    void start();

    void stop();

    /**
     * 注册服务
     *
     * @param serviceName  服务名
     * @param versionName 版本号
     */
    void registerService(String serviceName, String versionName);

    /**
     * 卸载服务
     * @param serviceName
     * @param versionName
     */
    void unregisterService(String serviceName, String versionName);

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
     * 获取某服务的runtime节点信息
     * @param serviceName
     * @return
     */
    List<RuntimeInstance> getRuntimeInstances(String serviceName);

    /**
     * 获取配置
     *
     * @param usingFallback
     * @param serviceKey
     * @return
     */
    ZkServiceInfo getConfig(boolean usingFallback, String serviceKey);


    /**
     * 获取限流规则
     *
     * @param usingFallback
     * @param serviceKey
     * @return
     * @see com.github.dapeng.impl.plugins.netty
     */
    List<FreqControlRule> getFreqControlRule(boolean usingFallback, String serviceKey);

    /**
     * 服务节点对外暂停/恢复提供服务
     */
    void pause(String serviceName, String versionName);
    void resume(String serviceName, String versionName);
}
