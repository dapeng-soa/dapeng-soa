package com.github.dapeng.registry;

import com.github.dapeng.core.enums.LoadBalanceStrategy;
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
public class RegistryDataUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryDataUtils.class);

    /**
     * 兼容
     *
     * @param data         byte数组
     * @param registerInfo 注册信息
     * @param isGlobal     全局性
     */
    public static void processZkConfig(byte[] data, RegisterInfo registerInfo, boolean isGlobal) {
        try {
            String configData = new String(data, "UTF-8");
            processZkConfig(configData, registerInfo, isGlobal);
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    /**
     * new get config data
     * <p>
     * timeout/800ms,createSupplier:100ms,modifySupplier:200ms;
     * loadbalance/LeastActive,createSupplier:Random,modifySupplier:RoundRobin;
     *
     * @param registerInfo
     */
    public static void processZkConfig(String configData, RegisterInfo registerInfo, boolean isGlobal) {
        String[] properties = configData.split(";");

        for (String property : properties) {
            String typeValue = property.split("/")[0];
            if (typeValue.equals(ConfigKey.TimeOut.getValue())) {
                if (isGlobal) {
                    String value = property.split("/")[1];
                    registerInfo.timeConfig.globalConfig = timeHelper(value);
                } else {
                    String[] keyValues = property.split(",");
                    for (String keyValue : keyValues) {
                        String[] props;
                        if (keyValue.contains("/")) {
                            props = keyValue.split("/");
                        } else {
                            props = keyValue.split(":");
                        }
                        registerInfo.timeConfig.serviceConfigs.put(props[0], timeHelper(props[1]));
                    }
                }

            } else if (typeValue.equals(ConfigKey.LoadBalance.getValue())) {

                if (isGlobal) {
                    String value = property.split("/")[1];
                    registerInfo.loadbalanceConfig.globalConfig = LoadBalanceStrategy.findByValue(value);
                } else {

                    String[] keyValues = property.split(",");
                    for (String keyValue : keyValues) {
                        String[] props;
                        if (keyValue.contains("/")) {
                            props = keyValue.split("/");
                        } else {
                            props = keyValue.split(":");
                        }
                        registerInfo.loadbalanceConfig.serviceConfigs.put(props[0], LoadBalanceStrategy.findByValue(props[1]));
                    }
                }
            }
        }
        LOGGER.info("get config from {} with data [{}]", registerInfo.service, configData);
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
}
