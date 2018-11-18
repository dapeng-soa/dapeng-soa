package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.ClientHandle;
import com.github.dapeng.core.ZkServiceInfo;
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
    private static final Map<String, ClientHandle.ClientHandleWeakRef> handlesByName = new ConcurrentHashMap<>(128);
    private static final ReferenceQueue<ClientHandle> referenceQueue = new ReferenceQueue<>();

    private ClientRefManager() {
        cleanThread.setDaemon(true);
        cleanThread.start();
    }

    public static ClientRefManager getInstance() {
        return instance;
    }

    public ClientHandle registerClient(String serviceName, String version) {
        ClientHandle.ClientHandleWeakRef wr = handlesByName.get(serviceName);
        ClientHandle handle;
        if (wr != null) {
            handle = wr.get();
            if (handle != null) {
                return handle;
            }
        }

        // todo: one lock per service
        synchronized (this) {
            LOGGER.debug("ClientRefManager::registerClient, serviceName:" + serviceName);
            handle = new ClientHandle(new ZkServiceInfo(serviceName, new CopyOnWriteArrayList<>()), version);
            clientZkAgent.sync(handle.serviceInfo());

            ClientHandle.ClientHandleWeakRef weakRef = new ClientHandle.ClientHandleWeakRef(handle, referenceQueue);
            handlesByName.put(serviceName, weakRef);
        }

        return handle;
    }

    public void onGcCallback(ClientHandle.ClientHandleWeakRef ref) {
        clientZkAgent.cancel(ref.serviceInfo());
    }


    Thread cleanThread = new Thread(() -> {
        while (true) {
            try {
                ClientHandle.ClientHandleWeakRef clientHandleRef = (ClientHandle.ClientHandleWeakRef) referenceQueue.remove(1000);
                if (clientHandleRef == null) continue;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("client for service:" + clientHandleRef.serviceName() + " is gone.");
                }

                onGcCallback(clientHandleRef);
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }, "dapeng-client-gc-monitor-thread");
}

