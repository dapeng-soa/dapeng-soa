package com.github.dapeng.registry;

import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.route.Route;

import java.util.List;
import java.util.Map;

/**
 * Registry Agent
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
     * 加载匹配的服务
     *
     * @param serviceName 服务名称
     * @param versionName 版本名称
     * @param compatible  是否兼容模式
     * @return
     */
    ServiceInfos loadMatchedServices(String serviceName, String versionName, boolean compatible);

    /**
     * 获取配置
     *
     * @param usingFallback
     * @param serviceKey
     * @return
     */
    Map<ConfigKey, Object> getConfig(boolean usingFallback, String serviceKey);

    /**
     * 获取路由规则
     *
     * @param usingFallback
     * @return
     */
    List<Route> getRoutes(boolean usingFallback);

    // todo 完善
    void registerConfig(ZkNodeConfigContext configs, String serverName, String versionName);


    void registerAllConfig(ZkNodeConfigContext configs);
}
