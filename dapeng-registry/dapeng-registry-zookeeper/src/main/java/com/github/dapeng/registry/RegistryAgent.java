package com.github.dapeng.registry;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;

import java.util.List;
import java.util.Map;

/**
 * Container Registry Agent
 *
 * @author craneding
 * @date 16/3/1
 */
public interface RegistryAgent {

    void start();

    void stop();

    /**
     * 注册服务
     *
     * @param serverName  服务名
     * @param versionName 版本号
     */
    void registerService(String serverName, String versionName);

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
}
