package com.github.dapeng.zookeeper.common;

import java.util.HashMap;

/**
 * ZK 配置
 *
 * @author huyj
 * @Created 2018/5/25 16:04
 */
public class ZkConfig {
    public String globalConfig = null;
    //服务级配置
    public String serviceConfig = null;
    //方法级别配置
    public HashMap<String, String> methodMap = new HashMap<>(16);

    /*
     * 优先级  全局配置 < 服务配置 < 方法级配置
     * */
    public Object getConfig(String method, Object defaultValue) {
        if (method != null && methodMap.containsKey(method)) {
            return methodMap.get(method);
        }
        if (serviceConfig != null) {
            return serviceConfig;
        }
        if (globalConfig != null) {
            return globalConfig;
        }
        return defaultValue;
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


    public void setGlobalConfig(String value) {
        this.globalConfig = value;
    }

    public void setServiceConfig(String value) {
        this.serviceConfig = value;
    }

    public void setMethodConfig(String method, String value) {
        this.methodMap.put(method, value);
    }

    public void removeGlobalConfig() {
        this.globalConfig = null;
    }

    public void removeServiceConfig() {
        this.serviceConfig = null;
    }

    public void removeMethodConfig(String method) {
        this.methodMap.remove(method);
    }
}
