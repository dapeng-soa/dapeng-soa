package com.github.dapeng.core;

public class ServiceInfo {

    public final String serviceName;
    public final String version;
    //task, commonService. cron..etc.
    public final String serviceType;
    public final CustomConfigInfo customConfigInfo;

    /**
     * 用于Task 拿到对应的 class 类型
     * 方便查找 对应类型的信息
     */
    public final Class<?> ifaceClass;

    public ServiceInfo(String serviceName, String version,CustomConfigInfo customConfigInfo,String serviceType, Class<?> ifaceClass){
        this.serviceName = serviceName;
        this.version = version;
        this.customConfigInfo = customConfigInfo;
        this.serviceType = serviceType;
        this.ifaceClass = ifaceClass;
    }

    @Override
    public String toString() {
        return serviceName + ":" + version + "@" + serviceType + "-" + ifaceClass.getName() +"timeout:"+customConfigInfo.timeout;
    }

}
