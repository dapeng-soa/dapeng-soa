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
package com.github.dapeng.registry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author tangliu
 * @date 2016/1/15
 */
public class ServiceInfo {

    public final String versionName;

    public final String host;

    public final int port;

    private AtomicInteger activeCount;

    public ServiceInfo(String host, Integer port, String versionName) {

        this.versionName = versionName;
        this.host = host;
        this.port = port;

        this.activeCount = new AtomicInteger(0);
    }

    public boolean equalTo(ServiceInfo sinfo) {
        if (!versionName.equals(sinfo.versionName))
            return false;

        if (!host.equals(sinfo.host))
            return false;

        if (port != sinfo.port)
            return false;

        return true;
    }

    public AtomicInteger getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(AtomicInteger activeCount) {
        this.activeCount = activeCount;
    }
}
