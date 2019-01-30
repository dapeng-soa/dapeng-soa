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
package com.github.dapeng.counter.util;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;

/**
 * @author with struy.
 * Create by 2018/2/7 00:43
 * email :yq1724555319@gmail.com
 */

public class CounterServiceProperties extends SoaSystemEnvProperties {
    private static final String KEY_SOA_COUNTER_INFLUXDB_URL = "soa.counter.influxdb.url";
    private static final String KEY_SOA_COUNTER_INFLUXDB_USER  = "counter.influxdb.user";
    private static final String KEY_SOA_COUNTER_INFLUXDB_PWD  = "counter.influxdb.pwd";

    public static String SOA_COUNTER_INFLUXDB_URL = get(KEY_SOA_COUNTER_INFLUXDB_URL,"http://127.0.0.1:8086");
    public static String SOA_COUNTER_INFLUXDB_USER = get(KEY_SOA_COUNTER_INFLUXDB_USER,"admin");
    public static String SOA_COUNTER_INFLUXDB_PWD = get(KEY_SOA_COUNTER_INFLUXDB_PWD,"admin");
}
