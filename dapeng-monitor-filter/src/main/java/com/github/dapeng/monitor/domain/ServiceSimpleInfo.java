package com.github.dapeng.monitor.domain;

import java.util.Objects;

/**
 * author with struy.
 * Create by 2018/1/31 11:44
 * email :yq1724555319@gmail.com
 */

public class ServiceSimpleInfo {
    private final String serviceName;
    private final String methodName;
    private final String versionName;

    public ServiceSimpleInfo(String serviceName, String methodName, String versionName) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.versionName = versionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceSimpleInfo that = (ServiceSimpleInfo) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(versionName, that.versionName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(serviceName, methodName, versionName);
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", versionName='" + versionName + '\'' +
                '}';
    }
}
