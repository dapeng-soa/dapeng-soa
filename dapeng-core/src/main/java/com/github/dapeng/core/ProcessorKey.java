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

/**
 * Created by tangliu on 2016/3/29.
 */
public class ProcessorKey {

    public ProcessorKey(String serviceName, String versionName) {
        this.serviceName = serviceName;
        this.versionName = versionName;
    }

    public final  String serviceName;

    public final String versionName;

    @Override
    public int hashCode() {
        return serviceName.hashCode() + versionName.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof ProcessorKey) {
            ProcessorKey target = (ProcessorKey) o;

            if (target.serviceName.equals(this.serviceName) && target.versionName.equals(this.versionName)) {
                return true;
            }
        }

        return false;
    }
}
