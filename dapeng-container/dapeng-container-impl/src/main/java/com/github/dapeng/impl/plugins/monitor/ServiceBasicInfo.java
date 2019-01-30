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
package com.github.dapeng.impl.plugins.monitor;

import java.util.Objects;

/**
 * @author with struy.
 * Create by 2018/1/31 11:44
 * email :yq1724555319@gmail.com
 */

public final class ServiceBasicInfo {
    private final String serviceName;
    private final String methodName;
    private final String versionName;

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getVersionName() {
        return versionName;
    }

    public ServiceBasicInfo(String serviceName, String methodName, String versionName) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.versionName = versionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ServiceBasicInfo)) {
            return false;
        }
        ServiceBasicInfo that = (ServiceBasicInfo) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(versionName, that.versionName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(serviceName, methodName, versionName);
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", versionName='" + versionName + '\'' +
                '}';
    }
}
