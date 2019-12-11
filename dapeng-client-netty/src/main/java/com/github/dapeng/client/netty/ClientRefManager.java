/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.registry.zookeeper.ClientZkAgent;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
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
        SoaConnectionPoolImpl.ClientInfoSoftRef softRef = handlesByName.get(serviceName+":"+version);
        SoaConnectionPool.ClientInfo clientInfo;
        if (softRef != null) {
            clientInfo = softRef.get();
            if (clientInfo != null) {
                return clientInfo;
            }
        }

        // todo: one lock per service
        synchronized (this) {
            LOGGER.debug("ClientRefManager::registerClient, serviceName:{},version:{}" , serviceName,version);
            clientInfo = new SoaConnectionPool.ClientInfo(serviceName, version);
            ZkServiceInfo serviceInfo = new ZkServiceInfo(serviceName, new CopyOnWriteArrayList<>());
            clientZkAgent.sync(serviceInfo);

            SoaConnectionPoolImpl.ClientInfoSoftRef clientInfoSoftRef = new SoaConnectionPoolImpl.ClientInfoSoftRef(clientInfo, serviceInfo, referenceQueue);
            handlesByName.put(serviceName+":"+version, clientInfoSoftRef);
        }

        return clientInfo;
    }

    public ZkServiceInfo serviceInfo(String serviceName) {
        return clientZkAgent.serviceInfo(serviceName);
    }

    private void onGcCallback(SoaConnectionPoolImpl.ClientInfoSoftRef ref) {
        Iterator<String> iterator = handlesByName.keySet().iterator();
        while(iterator.hasNext()){
            String key = iterator.next();
            if(key.contains(ref.serviceName)){
                iterator.remove();
            }
        }
        clientZkAgent.cancel(ref.serviceInfo);
    }


    Thread cleanThread = new Thread(() -> {
        while (true) {
            try {
                SoaConnectionPoolImpl.ClientInfoSoftRef clientInfoRef = (SoaConnectionPoolImpl.ClientInfoSoftRef) referenceQueue.remove(1000);
                if (clientInfoRef == null) continue;

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("client for service:" + clientInfoRef.serviceName + ":"+clientInfoRef.version+" is gone.");
                }

                onGcCallback(clientInfoRef);
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }, "dapeng-client-gc-monitor-thread");
}

