package com.github.dapeng.registry.zookeeper2;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * 客户端句柄
 * @author ever
 */
public class ClientHandle {
    private final ZkServiceInfo serviceInfo;


    public ClientHandle(final ZkServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public ZkServiceInfo serviceInfo() {
        return serviceInfo;
    }

    static class ClientHandleWeakRef extends WeakReference<ClientHandle> {
        private final ZkServiceInfo serviceInfo;

        public ClientHandleWeakRef(ClientHandle referent, ReferenceQueue<? super ClientHandle> q) {
            super(referent, q);
            this.serviceInfo = referent.serviceInfo();
        }

        public ZkServiceInfo serviceInfo() {
            return serviceInfo;
        }
    }
}
