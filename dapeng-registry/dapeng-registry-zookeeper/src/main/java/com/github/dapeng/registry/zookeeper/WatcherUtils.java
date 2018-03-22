package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.*;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author tangliu
 * @date 2016/8/8
 */
public class WatcherUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WatcherUtils.class);

    /*public static void processConfigData(String configNode, byte[] data, Map<String, Map<ConfigKey, Object>> config) {
        try {
            String propertiesStr = new String(data, "utf-8");

            String[] properties = propertiesStr.split(";");

            Map<ConfigKey, Object> propertyMap = new HashMap<>(properties.length);

            for (String property : properties) {

                String[] key_values = property.split("=");
                if (key_values.length == 2) {

                    ConfigKey type = ConfigKey.findByValue(key_values[0]);
                    switch (type) {

                        case Thread:
                            Integer value = Integer.valueOf(key_values[1]);
                            propertyMap.put(type, value);
                            break;
                        case ThreadPool:
                            Boolean bool = Boolean.valueOf(key_values[1]);
                            propertyMap.put(type, bool);
                            break;
                        case ClientTimeout:
                            long clientTimeout = Long.valueOf(key_values[1]);
                            propertyMap.put(type, clientTimeout);
                            break;
                        case ServerTimeout:
                            long serverTimeout = Long.valueOf(key_values[1]);
                            propertyMap.put(type, serverTimeout);
                            break;
                        case LoadBalance:
                            propertyMap.put(type, key_values[1]);
                            break;
                        case FailOver:
                            propertyMap.put(type, Integer.valueOf(key_values[1]));
                            break;
                        case Compatible:
                            propertyMap.put(type, key_values[1].split(","));
                            break;
                        default:
                            //just skip
                    }
                }
            }
            config.put(configNode, propertyMap);
            LOGGER.info("get config form {} with data [{}]", configNode, propertiesStr);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }*/

    /**
     * new get config data
     *
     *              timeout/800ms,createSupplier:100ms,modifySupplier:200ms;
                    loadbalance/LeastActive,createSupplier:Random,modifySupplier:RoundRobin;
     *
     *
     * @param data
     * @param zkInfo
     */
    public static void processZkConfig(byte[] data, ZkConfigInfo zkInfo, boolean isGlobal) {
        try {

            String configData = new String(data, "utf-8");

            String[] properties = configData.split(";");

            for (String property : properties) {
                String typeValue = property.split("/")[0];
                if (typeValue.equals(ConfigKey.TimeOut.getValue())) {
                    if (isGlobal) {
                        String value = property.split("/")[1];
                        zkInfo.timeConfig.globalConfig = timeHelper(value);
                    } else {
                        String[] keyValues = property.split(",");
                        for (String keyValue : keyValues) {
                            String[] props;
                            if (keyValue.contains("/")) {
                                props = keyValue.split("/");
                            } else {
                                props = keyValue.split(":");
                            }
                            zkInfo.timeConfig.serviceConfigs.put(props[0], timeHelper(props[1]));
                        }
                    }

                } else if (typeValue.equals(ConfigKey.LoadBalance.getValue())) {

                    if (isGlobal) {
                        String value = property.split("/")[1];
                        zkInfo.loadbalanceConfig.globalConfig = LoadBalanceStrategy.findByValue(value);
                    } else {

                        String[] keyValues = property.split(",");
                        for (String keyValue : keyValues) {
                            String[] props;
                            if (keyValue.contains("/")) {
                                props = keyValue.split("/");
                            } else {
                                props = keyValue.split(":");
                            }
                            zkInfo.loadbalanceConfig.serviceConfigs.put(props[0], LoadBalanceStrategy.findByValue(props[1]));
                        }
                    }
                }
            }
            LOGGER.info("get config form {} with data [{}]");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * serviceName下子节点列表即可用服务地址列表
     * 子节点命名为：host:port:versionName
     *
     * @param serviceName
     * @param path
     * @param infos
     */
    public static void resetServiceInfoByName(String serviceName, String path, List<String> infos, Map<String, List<ServiceInfo>> caches) {
        LOGGER.info(serviceName + "   " + infos);
        List<ServiceInfo> sinfos = new ArrayList<>();

        for (String info : infos) {
            String[] serviceInfo = info.split(":");
            ServiceInfo sinfo = new ServiceInfo(serviceInfo[0], Integer.valueOf(serviceInfo[1]), serviceInfo[2]);
            sinfos.add(sinfo);
        }

        if (caches.containsKey(serviceName)) {
            List<ServiceInfo> currentInfos = caches.get(serviceName);

            for (ServiceInfo sinfo : sinfos) {
                for (ServiceInfo currentSinfo : currentInfos) {
                    if (sinfo.equalTo(currentSinfo)) {
                        sinfo.setActiveCount(currentSinfo.getActiveCount());
                        break;
                    }
                }
            }
        }
        caches.put(serviceName, sinfos);
    }

    public static Long timeHelper(String number) {
        number = number.replaceAll("[^(0-9)]", "");
        return Long.valueOf(number);
    }
}
