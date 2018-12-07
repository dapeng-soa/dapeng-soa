package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.registry.zookeeper.ClientZkAgent;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ever
 */
public class ClientRefManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRefManager.class);

    private static final ClientRefManager instance = new ClientRefManager();
    private final ClientZkAgent clientZkAgent = ClientZkAgent.getInstance();

    /**
     * in only, never out
     */
    private static final Map<String, SoaConnectionPoolImpl.ClientInfoSoftRef> handlesByName = new ConcurrentHashMap<>(128);
    private static final ReferenceQueue<SoaConnectionPool.ClientInfo> referenceQueue = new ReferenceQueue<>();

    private ClientRefManager() {
        cleanThread.setDaemon(true);
        cleanThread.start();
    }

    public static ClientRefManager getInstance() {
        return instance;
    }

    public SoaConnectionPool.ClientInfo registerClient(String serviceName, String version) {
        SoaConnectionPoolImpl.ClientInfoSoftRef softRef = handlesByName.get(serviceName);
        SoaConnectionPool.ClientInfo clientInfo;
        if (softRef != null) {
            clientInfo = softRef.get();
            if (clientInfo != null) {
                return clientInfo;
            }
        }

        // todo: one lock per service
        synchronized (this) {
            LOGGER.debug("ClientRefManager::registerClient, serviceName:" + serviceName);
            clientInfo = new SoaConnectionPool.ClientInfo(serviceName, version);
            ZkServiceInfo serviceInfo = new ZkServiceInfo(serviceName, new CopyOnWriteArrayList<>());
            clientZkAgent.sync(serviceInfo);

            SoaConnectionPoolImpl.ClientInfoSoftRef clientInfoSoftRef = new SoaConnectionPoolImpl.ClientInfoSoftRef(clientInfo, serviceInfo, referenceQueue);
            handlesByName.put(serviceName, clientInfoSoftRef);
        }

        return clientInfo;
    }

    public ZkServiceInfo serviceInfo(String serviceName) {
        return clientZkAgent.serviceInfo(serviceName);
    }

    private void onGcCallback(SoaConnectionPoolImpl.ClientInfoSoftRef ref) {
        clientZkAgent.cancel(ref.serviceInfo);
    }


    Thread cleanThread = new Thread(() -> {
        while (true) {
            try {
                SoaConnectionPoolImpl.ClientInfoSoftRef clientInfoRef = (SoaConnectionPoolImpl.ClientInfoSoftRef) referenceQueue.remove(1000);
                if (clientInfoRef == null) continue;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("client for service:" + clientInfoRef.serviceName + " is gone.");
                }

                onGcCallback(clientInfoRef);
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }, "dapeng-client-gc-monitor-thread");
}

