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
