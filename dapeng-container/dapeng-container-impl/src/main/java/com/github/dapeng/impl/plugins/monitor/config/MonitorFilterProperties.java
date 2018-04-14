package com.github.dapeng.impl.plugins.monitor.config;


import com.github.dapeng.core.helper.SoaSystemEnvProperties;

public class MonitorFilterProperties extends SoaSystemEnvProperties {
    private static final String KEY_SOA_MONITOR_INFLUXDB_DATABASE = "soa.monitor.infulxdb.database";
    private static final String KEY_SOA_MONITOR_SERVICE_PROCESS_PERIOD = "soa.monitor.service.period";

    public static String SOA_MONITOR_INFLUXDB_DATABASE = get(KEY_SOA_MONITOR_INFLUXDB_DATABASE, "dapengState");
    public static Integer SOA_MONITOR_SERVICE_PROCESS_PERIOD = Integer.valueOf(get(KEY_SOA_MONITOR_SERVICE_PROCESS_PERIOD, "60"));

    public static Boolean SOA_JMX_SWITCH_MONITOR = SOA_MONITOR_ENABLE;
}
