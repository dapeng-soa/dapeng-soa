package com.github.dapeng.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServiceInfo {

    public final String serviceName;
    public final String version;
    //task, commonService. cron..etc.
    public final String serviceType;

    public final Optional<CustomConfigInfo> configInfo;

    public final Map<String, Optional<CustomConfigInfo>> methodsMap;

    public final HashMap<String, Long> methodsMaxProcessTimeMap;

    public final Map<String,TCC> tccMap;
    /**
     * 用于Task 拿到对应的 class 类型
     * 方便查找 对应类型的信息
     */
    public final Class<?> ifaceClass;

    public ServiceInfo(String serviceName, String version, String serviceType, Class<?> ifaceClass, Optional<CustomConfigInfo> configInfo, Map<String, Optional<CustomConfigInfo>> methodsMap, HashMap<String, Long> methodsMaxProcessTimeMap,Map<String,TCC> tccMap) {
        this.serviceName = serviceName;
        this.version = version;
        this.serviceType = serviceType;
        this.ifaceClass = ifaceClass;
        this.configInfo = configInfo;
        this.methodsMap = methodsMap;
        this.methodsMaxProcessTimeMap = methodsMaxProcessTimeMap;
        this.tccMap=tccMap;
    }

    @Override
    public String toString() {
        return serviceName + ":" + version + "@" + serviceType + "-" + ifaceClass.getName();
    }

}
