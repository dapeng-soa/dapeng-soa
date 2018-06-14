package com.github.dapeng.zookeeper.common;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.Weight;
import com.github.dapeng.router.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端 Zk Context
 *
 * @author huyj
 * @Created 2018/5/25 13:26
 */
public class ZkDataContext {

    /**
     * 服务信息
     * key  [serviceName]
     * value  [List<ZkServiceInfo>]
     */
    private final ConcurrentHashMap<String, List<ZkServiceInfo>> servicesMap = new ConcurrentHashMap<>(16);


    /**
     * 服务运行  实例信息
     * <p>
     * key  [serviceName]
     * value  [List<RuntimeInstance>]
     */
    private final ConcurrentHashMap<String, List<RuntimeInstance>> runtimeInstancesMap = new ConcurrentHashMap<>(16);

    /**
     * config配置信息
     * key  [serviceName]
     * value  [Config]
     * <p>
     * <p>
     * 全局配置 /soa/config/services timeout/800ms;loadBalance/random
     * 服务级别和方法级别   timeout/800ms,register:4001ms,modifySupplier:200ms;loadBalance/leastActive,createSupplier:random,modifySupplier:roundRobin;
     */
    private final ConcurrentHashMap<String, HashMap<ConfigKey, ZkConfig>> configsMap = new ConcurrentHashMap<>(16);

    /**
     * 路由配置信息 key = serviceName
     * key [serviceName]
     * value [List<Route>]
     */
    private final ConcurrentHashMap<String, List<Route>> routesMap = new ConcurrentHashMap<>(16);


    /**
     * 限流配置信息
     * key [serviceName]
     * value [FreqControlRule]
     */
    private final ConcurrentHashMap<String, List<FreqControlRule>> freqRulesMap = new ConcurrentHashMap<>(16);


    /**
     * 白名单配置 列表
     */
    private final List<String> whiteList = new ArrayList<>();
    /**
     * 权重配置信息
     */
    private final ConcurrentHashMap<String,List<Weight>> weightMap = new ConcurrentHashMap<>(16);


    /********setter getter *************************************/
    public ConcurrentHashMap<String, List<ZkServiceInfo>> getServicesMap() {
        return servicesMap;
    }

    public ConcurrentHashMap<String, HashMap<ConfigKey, ZkConfig>> getConfigsMap() {
        return configsMap;
    }

    public ConcurrentHashMap<String, List<Route>> getRoutesMap() {
        return routesMap;
    }

    public ConcurrentHashMap<String, List<FreqControlRule>> getFreqRulesMap() {
        return freqRulesMap;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public ConcurrentHashMap<String, List<RuntimeInstance>> getRuntimeInstancesMap() {
        return runtimeInstancesMap;
    }
    public ConcurrentHashMap<String, List<Weight>> getWeightMap() {
        return weightMap;
    }

    public void setConfigData(ConfigLevel configLevel, String serviceName, String method, ConfigKey configKey, String configData) {
        switch (configLevel) {
            case GLOBAL:
                if (Objects.isNull(configData)) {
                    //foreach 不能改变原值
                    //zkDataContext.getConfigsMap().values().forEach(zkConfigMap -> zkConfigMap.get(itemKey).removeGlobalConfig(itemKey.getValue()));
                    for (HashMap<ConfigKey, ZkConfig> cMap : getConfigsMap().values()) {
                        cMap.get(configKey).removeGlobalConfig();
                    }
                } else {
                    //TODO 全局变量 在初始化时 有BUG eg:先安同步全局变量  后同步服务和方法会有问题
                    ZkConfig global_config = null;
                    /*   if (getConfigsMap().values().isEmpty()) {
                        global_config = new ZkConfig();
                        global_config.setGlobalConfig(configData);
                        HashMap<ConfigKey, ZkConfig> configMap = new HashMap<>();
                        configMap.put(configKey, global_config);
                        getConfigsMap().put(serviceName, configMap);
                        return;
                    }*/
                    for (HashMap<ConfigKey, ZkConfig> cMap : getConfigsMap().values()) {
                        global_config = cMap.get(configKey);
                        global_config = Objects.isNull(global_config) ? new ZkConfig() : global_config;
                        global_config.setGlobalConfig(configData);
                        cMap.put(configKey, global_config);
                    }
                }
                break;
            case SERVICE:
                HashMap<ConfigKey, ZkConfig> service_Map = Objects.isNull(getConfigsMap().get(serviceName)) ? new HashMap<ConfigKey, ZkConfig>() : getConfigsMap().get(serviceName);
                ZkConfig service_config = service_Map.get(configKey);
                service_config = Objects.isNull(service_config) ? new ZkConfig() : service_config;
                if (Objects.isNull(configData)) {
                    service_config.removeServiceConfig();
                } else {
                    service_config.setServiceConfig(configData);
                }
                service_Map.put(configKey, service_config);
                getConfigsMap().put(serviceName, service_Map);
                break;
            case METHOD:
                HashMap<ConfigKey, ZkConfig> method_Map = Objects.isNull(getConfigsMap().get(serviceName)) ? new HashMap<ConfigKey, ZkConfig>() : getConfigsMap().get(serviceName);
                ZkConfig method_config = method_Map.get(configKey);
                method_config = Objects.isNull(method_config) ? new ZkConfig() : method_config;
                if (Objects.isNull(configData)) {
                    method_config.removeMethodConfig(method);
                } else {
                    method_config.setMethodConfig(method, configData);
                }
                method_Map.put(configKey, method_config);
                getConfigsMap().put(serviceName, method_Map);
                break;

            default:
                break;
        }
    }

    public void setWeightMap(String serviceName,List<Weight> weights){

        this.weightMap.put(serviceName,weights);
    }

    /**********ConfigLevel  enum**********/
    public enum ConfigLevel {
        GLOBAL, SERVICE, METHOD
    }

}
