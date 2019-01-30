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
package com.github.dapeng.core;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代表一个服务的运行实例
 *
 * @author lihuimin
 * @date 2017/12/25
 */
public class RuntimeInstance {

    public final String service;
    public final String version;
    public final String ip;
    public final int port;
    public int weight;

    /**
     * 该服务实例在某客户端的调用计数
     */
    private AtomicInteger activeCount = new AtomicInteger(0);

    public RuntimeInstance(String service, String ip, int port, String version) {
        this.service = service;
        this.version = version;
        this.ip = ip;
        this.port = port;
        this.weight = SoaSystemEnvProperties.SOA_INSTANCE_WEIGHT;
    }

    public int getActiveCount() {
        return activeCount.get();
    }

    /**
     * 调用计数+1
     *
     * @return 操作后的计数值
     */
    public int increaseActiveCount() {
        return activeCount.incrementAndGet();
    }

    /**
     * 调用计数-1
     *
     * @return 操作后的计数值
     */
    public int decreaseActiveCount() {
        return activeCount.decrementAndGet();
    }

    @Override
    public String toString() {
        return "IP:【" + ip + ":" + port + ":" + version + '】';
    }
}
