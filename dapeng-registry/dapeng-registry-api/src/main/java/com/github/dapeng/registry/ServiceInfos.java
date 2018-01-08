package com.github.dapeng.registry;

import java.util.List;

/**
 * Created by tangliu on 2016/8/12.
 */
public class ServiceInfos {

    public ServiceInfos(boolean usingFallbackZk, List<ServiceInfo> serviceInfos) {
        this.usingFallbackZk = usingFallbackZk;
        this.serviceInfoList = serviceInfos;
    }

    public boolean usingFallbackZk = false;

    List<ServiceInfo> serviceInfoList;

    public boolean isUsingFallbackZk() {
        return usingFallbackZk;
    }

    public void setUsingFallbackZk(boolean usingFallbackZk) {
        this.usingFallbackZk = usingFallbackZk;
    }

    public List<ServiceInfo> getServiceInfoList() {
        return serviceInfoList;
    }

    public void setServiceInfoList(List<ServiceInfo> serviceInfoList) {
        this.serviceInfoList = serviceInfoList;
    }
}
