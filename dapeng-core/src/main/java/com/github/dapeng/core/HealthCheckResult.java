package com.github.dapeng.core;

import com.github.dapeng.core.enums.ServiceHealthStatus;

/**
 * @author huyj
 * @Created 2018-11-07 14:53
 */
public class HealthCheckResult {

    private Class<?> serviceClass ;
    private ServiceHealthStatus healthStatus;
    private String remark;

    public HealthCheckResult(Class<?> serviceClass, ServiceHealthStatus healthStatus, String remark) {
        this.serviceClass = serviceClass;
        this.healthStatus = healthStatus;
        this.remark = remark;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public ServiceHealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(ServiceHealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
