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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huyj
 * @Created 2018-08-21 16:40
 */
class SubPoolFactory {

    private final static Map<IpPort, SubPool> subPoolsMap = new HashMap<>(16);
    private static final ReentrantLock subPoolLock = new ReentrantLock();

    static SubPool getSubPool(String ip, int port) {
        IpPort ipPort = new IpPort(ip, port);
        SubPool subPool = subPoolsMap.get(ipPort);
        if (subPool == null) {
            try {
                subPoolLock.lock();
                subPool = subPoolsMap.get(ipPort);
                if (subPool == null) {
                    subPool = new SubPool(ip, port);
                    subPoolsMap.put(ipPort, subPool);
                }
            } finally {
                subPoolLock.unlock();
            }
        }
        return subPool;
    }
}
