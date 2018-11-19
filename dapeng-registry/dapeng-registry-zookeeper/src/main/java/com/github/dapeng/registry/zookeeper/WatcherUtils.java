package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.Weight;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
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

    /**
     * new get config data
     * <p>
     * timeout/800ms,createSupplier:100ms,modifySupplier:200ms
     * loadbalance/LeastActive,createSupplier:Random,modifySupplier:RoundRobin
     * weight/192.168.4.107/9095/700  service weight config1
     * weight/192.168.4.107/500       service weight config2
     * weight/600                    global weight config
     *
     * @param data
     * @param zkInfo
     */
    public static void processZkConfig(byte[] data, ZkServiceInfo zkInfo, boolean isGlobal) {
        try {

            String configData = new String(data, "utf-8");

            String[] properties = configData.split("\n|\r|\r\n");

            if (!isGlobal && !zkInfo.weightServiceConfigs.isEmpty()) {
                zkInfo.weightServiceConfigs.clear();
            }
            for (String property : properties) {
                if (!"".equals(property)) {
                    String typeValue = property.split("/")[0];
                    if (typeValue.equals(ConfigKey.TimeOut.getValue())) {//服务超时 TimeOut
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
                    } else if (typeValue.equals(ConfigKey.ProcessTime.getValue())) { //服务慢服务检测 ProcessTime
                        if (isGlobal) {
                            String value = property.split("/")[1];
                            zkInfo.processTimeConfig.globalConfig = timeHelper(value);
                        } else {
                            String[] keyValues = property.split(",");
                            for (String keyValue : keyValues) {
                                String[] props;
                                if (keyValue.contains("/")) {
                                    props = keyValue.split("/");
                                } else {
                                    props = keyValue.split(":");
                                }
                                zkInfo.processTimeConfig.serviceConfigs.put(props[0], timeHelper(props[1]));
                            }
                        }
                    } else if (typeValue.equals(ConfigKey.LoadBalance.getValue())) { //负载均衡 LoadBalance
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
                    } else if (typeValue.equals(ConfigKey.Weight.getValue())) {//权重 Weight
                        if (isGlobal) {
                            zkInfo.weightGlobalConfig = doParseWeightData(property);

                        } else {
                            zkInfo.weightServiceConfigs.add(doParseWeightData(property));
                        }
                    }
                }
            }
            recalculateRuntimeInstanceWeight(zkInfo);
            LOGGER.info("get config from {} with data [{}]", zkInfo.serviceName(), configData);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * 将zk config 中的权重设置，同步到运行实例中
     *
     * @param zkInfo
     */
    public static void recalculateRuntimeInstanceWeight(ZkServiceInfo zkInfo) {
        if (zkInfo != null) {
            List<RuntimeInstance> runtimeInstances = zkInfo.runtimeInstances();
            if (runtimeInstances != null && runtimeInstances.size() > 0) {
                for (RuntimeInstance runtimeInstance : runtimeInstances) {
                    if (zkInfo.weightGlobalConfig.ip != null) {   //没有全局配置的情况下ip = null，有全局配置ip = ""
                        runtimeInstance.weight = zkInfo.weightGlobalConfig.weight;
                    }
                    if (zkInfo.weightServiceConfigs != null) {
                        List<Weight> weights = zkInfo.weightServiceConfigs;
                        for (Weight weight : weights) {
                            if (weight.ip.equals(runtimeInstance.ip)) {
                                if (weight.port == runtimeInstance.port) {
                                    runtimeInstance.weight = weight.weight;
                                    break;
                                } else if (weight.port == -1) {
                                    runtimeInstance.weight = weight.weight;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * parse zk weight config
     *
     * @param weightData
     * @return
     */
    public static Weight doParseWeightData(String weightData) {
        Weight weight = new Weight();
        String[] strArr = weightData.split("[/]");
        if (strArr.length >= 2) {
            if (strArr.length == 2) {                   //weight/600  global weight config
                weight.ip = "";
                weight.port = -1;
                weight.weight = Integer.parseInt(strArr[1]);
            } else if (strArr.length == 3) {              //weight/192.168.4.107/500       service weight config2
                weight.ip = strArr[1];
                weight.port = -1;
                weight.weight = Integer.parseInt(strArr[2]);
            } else {                                   //   weight/192.168.4.107/9095/700  service weight config1
                weight.ip = strArr[1];
                weight.port = Integer.parseInt(strArr[2]);
                weight.weight = Integer.parseInt(strArr[3]);
            }
        } else {
            weight = null;
        }
        return weight;
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

    /**
     * 将配置信息中的时间单位ms 字母替换掉  100ms -> 100
     *
     * @param number
     * @return
     */
    public static Long timeHelper(String number) {
        number = number.replaceAll("[^(0-9)]", "");
        return Long.valueOf(number);
    }


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
}
