package com.github.dapeng.core;

public class ServiceInfo {

    public final String serviceName;
    public final String version;
    public final String serviceType; //task, commonService. cron..etc.

    /**
     * 用于Task 拿到对应的 class 类型
     * 方便查找 对应类型的信息
     */
    public final Class<?> ifaceClass;

    public ServiceInfo(String serviceName, String version, String serviceType, Class<?> ifaceClass){
        this.serviceName = serviceName;
        this.version = version;

        this.serviceType = serviceType;
        this.ifaceClass = ifaceClass;
    }


}
