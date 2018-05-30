package com.github.dapeng.common;

import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.RuntimeInstance;
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
     */
    private final ConcurrentHashMap<String, HashMap<ConfigKey, ZkConfig>> configsMap = new ConcurrentHashMap<>(16);

    /**
     * 路由配置信息 key = serviceName
     * key [serviceName]
     * value [List<Route>]
     */
    private final ConcurrentHashMap<String, List<Route>> routesMap = new ConcurrentHashMap<>(16);


    /**
     * 路由配置信息
     * key [serviceName]
     * value [FreqControlRule]
     */
    private final ConcurrentHashMap<String, List<FreqControlRule>> freqRulesMap = new ConcurrentHashMap<>(16);


    /**
     * 白名单配置 列表
     */
    private final List<String> whiteList = new ArrayList<>();


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


    public void setConfigData(ConfigLevel configLevel, String serviceName, ConfigKey configKey, String configData) {
        switch (configLevel) {
            case GLOBAL:
                if (Objects.isNull(configData)) {
                    //foreach 不能改变原值
                    //zkDataContext.getConfigsMap().values().forEach(zkConfigMap -> zkConfigMap.get(itemKey).removeGlobalConfig(itemKey.getValue()));
                    for (HashMap<ConfigKey, ZkConfig> cMap : getConfigsMap().values()) {
                        cMap.get(configKey).removeGlobalConfig(configKey.getValue());
                    }
                } else {
                    //foreach 不能改变原值
                    //zkDataContext.getConfigsMap().values().forEach(zkConfigMap -> zkConfigMap.get(itemKey).setGlobalConfig(itemKey.getValue(), itemArr[1]));
                    for (HashMap<ConfigKey, ZkConfig> cMap : getConfigsMap().values()) {
                        cMap.get(configKey).setGlobalConfig(configKey.getValue(), configData);
                    }
                }
                break;
            case SERVICE:
                HashMap<ConfigKey, ZkConfig> service_Map = Objects.isNull(getConfigsMap().get(serviceName)) ? new HashMap<ConfigKey, ZkConfig>() : getConfigsMap().get(serviceName);
                ZkConfig service_config = service_Map.get(configKey);
                service_config = Objects.isNull(service_config) ? new ZkConfig() : service_config;
                if (Objects.isNull(configData)) {
                    service_config.removeServiceConfig(configKey.getValue());
                } else {
                    service_config.setServiceConfig(configKey.getValue(), configData);
                }
                service_Map.put(configKey, service_config);
                getConfigsMap().put(serviceName, service_Map);
                break;
            case METHOD:
                HashMap<ConfigKey, ZkConfig> method_Map = Objects.isNull(getConfigsMap().get(serviceName)) ? new HashMap<ConfigKey, ZkConfig>() : getConfigsMap().get(serviceName);
                ZkConfig method_config = method_Map.get(configKey);
                method_config = Objects.isNull(method_config) ? new ZkConfig() : method_config;
                if (Objects.isNull(configData)) {
                    method_config.removeMethodConfig(configKey.getValue());
                } else {
                    method_config.setMethodConfig(configKey.getValue(), configData);
                }
                method_Map.put(configKey, method_config);
                getConfigsMap().put(serviceName, method_Map);
                break;

            default:
                break;
        }
    }

    /**********ConfigLevel  enum**********/
    public enum ConfigLevel {
        GLOBAL, SERVICE, METHOD
    }

}
