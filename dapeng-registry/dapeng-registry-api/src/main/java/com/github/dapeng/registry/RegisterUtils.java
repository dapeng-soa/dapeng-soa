package com.github.dapeng.registry;

import com.github.dapeng.core.enums.LoadBalanceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * desc: 111
 *
 * @author hz.lei
 * @since 2018年07月20日 下午5:24
 */
public class RegisterUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterUtils.class);


    public static void processZkConfig(String configData, RegisterInfo zkInfo, boolean isGlobal) {
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
        LOGGER.info("get config from {} with data [{}]", zkInfo.service, configData);
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
