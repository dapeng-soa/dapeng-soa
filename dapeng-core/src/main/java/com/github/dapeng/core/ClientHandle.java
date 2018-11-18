package com.github.dapeng.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * 客户端句柄
 * @author ever
 */
public class ClientHandle {
    private final ZkServiceInfo serviceInfo;
    private final String version;

    public ClientHandle(final ZkServiceInfo serviceInfo, final String version) {
        this.serviceInfo = serviceInfo;
        this.version =version;
    }

    public ZkServiceInfo serviceInfo() {
        return serviceInfo;
    }

    public String serviceName() {
        return serviceInfo.serviceName();
    }

    public String version() {
        return version;
    }

    public static class ClientHandleWeakRef extends WeakReference<ClientHandle> {
        private final ZkServiceInfo serviceInfo;

        public ClientHandleWeakRef(ClientHandle referent, ReferenceQueue<? super ClientHandle> q) {
            super(referent, q);
            this.serviceInfo = referent.serviceInfo();
        }

        public ZkServiceInfo serviceInfo() {
            return serviceInfo;
        }

        public String serviceName() {
            return serviceInfo.serviceName();
        }
    }
}
