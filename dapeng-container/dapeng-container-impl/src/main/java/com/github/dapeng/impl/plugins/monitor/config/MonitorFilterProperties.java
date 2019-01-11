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
package com.github.dapeng.impl.plugins.monitor.config;


import com.github.dapeng.core.helper.SoaSystemEnvProperties;

public class MonitorFilterProperties extends SoaSystemEnvProperties {
    private static final String KEY_SOA_MONITOR_INFLUXDB_DATABASE = "soa.monitor.influxdb.database";
    private static final String KEY_SOA_MONITOR_SERVICE_PROCESS_PERIOD = "soa.monitor.service.period";
    private static final String KEY_SOA_JMXRMI_ENABLE = "soa.jmxrmi.enable";
    private static final String KEY_SOA_JMXRMI_PORT = "soa.jmxrmi.port";

    public static String SOA_MONITOR_INFLUXDB_DATABASE = get(KEY_SOA_MONITOR_INFLUXDB_DATABASE, "dapengState");
    public static Integer SOA_MONITOR_SERVICE_PROCESS_PERIOD = Integer.valueOf(get(KEY_SOA_MONITOR_SERVICE_PROCESS_PERIOD, "60"));
    public static Boolean SOA_JMXRMI_ENABLE = Boolean.valueOf(get(KEY_SOA_JMXRMI_ENABLE,"true"));
    public static Integer SOA_JMXRMI_PORT=Integer.valueOf(get(KEY_SOA_JMXRMI_PORT,"36524"));

    public static Boolean SOA_JMX_SWITCH_MONITOR = SOA_MONITOR_ENABLE;
}
