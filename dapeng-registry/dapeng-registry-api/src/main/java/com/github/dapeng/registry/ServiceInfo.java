package com.github.dapeng.registry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tangliu on 2016/1/15.
 */
public class ServiceInfo {

    public String versionName;

    public String host;

    public Integer port;

    public String getVersionName() {
        return versionName;
    }

    public AtomicInteger getActiveCount() {
        return activeCount;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public AtomicInteger activeCount;

    public void setActiveCount(AtomicInteger activeCount) {
        this.activeCount = activeCount;
    }

    public ServiceInfo(String host, Integer port, String versionName) {

        this.versionName = versionName;
        this.host = host;
        this.port = port;

        this.activeCount = new AtomicInteger(0);
    }

    public boolean equalTo(ServiceInfo sinfo) {
        if (!versionName.equals(sinfo.getVersionName()))
            return false;

        if (!host.equals(sinfo.getHost()))
            return false;

        if (port != sinfo.getPort())
            return false;

        return true;
    }

}
