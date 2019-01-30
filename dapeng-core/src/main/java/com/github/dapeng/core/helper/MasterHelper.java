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
package com.github.dapeng.core.helper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tangliu on 2016/7/13.
 */
public class MasterHelper {

    public static Map<String, Boolean> isMaster = new HashMap<>();

    /**
     * 根据serviceName, versionName，判断当前服务是否集群中的master
     *  todo 服务版本号是否作为master判断的依据??
     * @param servieName
     * @param versionName
     * @return
     */
    public static boolean isMaster(String servieName, String versionName) {

        String key = generateKey(servieName, versionName);

        if (!isMaster.containsKey(key))
            return false;
        else
            return isMaster.get(key);

    }

    public static String generateKey(String serviceName, String versionName) {
        return serviceName + ":" + versionName;
    }
}
