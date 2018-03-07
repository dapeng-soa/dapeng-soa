package com.github.dapeng.registry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lihuimin on 2017/12/25.
 */
public class RuntimeInstance {

    public final String service;
    public final String version;
    public final String ip;
    public final int port;

    private AtomicInteger activeCount = new AtomicInteger(0);

    public RuntimeInstance(String service, String ip, int port, String version) {
        this.service = service;
        this.version = version;
        this.ip = ip;
        this.port = port;
    }

    public AtomicInteger getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(AtomicInteger activeCount) {
        this.activeCount = activeCount;
    }
}
