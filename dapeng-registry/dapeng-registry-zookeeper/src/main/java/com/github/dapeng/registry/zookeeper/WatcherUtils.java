package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tangliu on 2016/8/8.
 */
public class WatcherUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WatcherUtils.class);

    public static void processConfigData(String configNode, byte[] data, Map<String, Map<ConfigKey, Object>> config) {
        Map<ConfigKey, Object> propertyMap = new HashMap<>();
        try {
            String propertiesStr = new String(data, "utf-8");

            String[] properties = propertiesStr.split(";");
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
                        case Timeout:
                            Integer timeout = Integer.valueOf(key_values[1]);
                            propertyMap.put(type, timeout);
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
                    }
                }
            }

            LOGGER.info("get config form {} with data [{}]", configNode, propertiesStr);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }

        config.put(configNode, propertyMap);
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
}
