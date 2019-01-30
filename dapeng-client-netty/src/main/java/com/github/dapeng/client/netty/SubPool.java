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

import com.github.dapeng.core.SoaConnection;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class SubPool {

    static final int MAX = SoaSystemEnvProperties.SOA_SUBPOOL_SIZE;

    private final String ip;
    private final int port;

    /**
     * connection that used by rpcClients, such as java, scala, php..
     */
    private final SoaConnection[] soaConnections;
    private final AtomicInteger index = new AtomicInteger(0);

    SubPool(String ip, int port) throws SoaException {
        this.ip = ip;
        this.port = port;

        soaConnections = new SoaConnection[MAX];
        for(int i = 0; i < MAX; i++) {
            soaConnections[i] = new SoaConnectionImpl(ip, port);
        }
    }

    public SoaConnection getConnection() {
        if (MAX == 1) {
            return soaConnections[0];
        }

        int idx = this.index.getAndIncrement();
        if(idx < 0) {
            synchronized (this){
                this.index.set(0);
                idx = 0;
            }
        }
        return soaConnections[idx%MAX];
    }
}