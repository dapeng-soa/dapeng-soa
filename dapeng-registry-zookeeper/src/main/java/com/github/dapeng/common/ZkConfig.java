package com.github.dapeng.common;

import java.util.HashMap;

/**
 * ZK 配置
 *
 * @author huyj
 * @Created 2018/5/25 16:04
 */
public class ZkConfig {

    /**
     * 全局配置 /soa/config/services timeout/800ms;loadBalance/random
     * 服务级别和方法级别   timeout/800ms,register:4001ms,modifySupplier:200ms;loadBalance/leastActive,createSupplier:random,modifySupplier:roundRobin;
     */
    //全局配置
    private final HashMap<String, String> globalMap = new HashMap<>(16);
    //服务级配置
    private final HashMap<String, String> serviceMap = new HashMap<>(16);
    //方法级别配置
    private final HashMap<String, String> methodMap = new HashMap<>(16);


    /*
     * 优先级  全局配置 < 服务配置 < 方法级配置
     * */
    public Object getConfig(String key, String method, Object defaultValue) {
        if (method != null && methodMap.containsKey(method)) {
            return methodMap.get(method);
        }
        if (key != null && serviceMap.containsKey(key)) {
            return serviceMap.get(key);
        }
        if (key != null && globalMap.containsKey(key)) {
            return globalMap.get(key);
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


    public void setGlobalConfig(String key, String value) {
        this.globalMap.put(key, value);
    }

    public void setServiceConfig(String key, String value) {
        this.serviceMap.put(key, value);
    }

    public void setMethodConfig(String key, String value) {
        this.methodMap.put(key, value);
    }


    public void removeGlobalConfig(String key) {
        this.globalMap.remove(key);
    }

    public void removeServiceConfig(String key) {
        this.serviceMap.remove(key);
    }

    public void removeMethodConfig(String key) {
        this.methodMap.remove(key);
    }
}
