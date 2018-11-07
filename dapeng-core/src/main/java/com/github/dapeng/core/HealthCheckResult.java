package com.github.dapeng.core;

import com.github.dapeng.core.enums.ServiceHealthStatus;

/**
 * @author huyj
 * @Created 2018-11-07 14:53
 */
public class HealthCheckResult {

    private String serviceName;
    private ServiceHealthStatus healthStatus;
    private String remark;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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
