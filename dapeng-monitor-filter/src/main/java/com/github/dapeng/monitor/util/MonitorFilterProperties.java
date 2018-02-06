package com.github.dapeng.monitor.util;

import com.github.dapeng.util.SoaSystemEnvProperties;

public class MonitorFilterProperties extends SoaSystemEnvProperties {
    private static final String KEY_SOA_MONITOR_INFLUXDB_DATABASE = "soa.monitor.infulxdb.database";
    private static final String KEY_SOA_MONITOR_QPS_PERIOD = "soa.monitor.qps.period";
    private static final String KEY_SOA_MONITOR_SERVICE_PROCESS_PERIOD = "soa.monitor.service.period";

    public static String SOA_MONITOR_INFLUXDB_DATABASE = get(KEY_SOA_MONITOR_INFLUXDB_DATABASE,"dapengState");
    public static Integer SOA_MONITOR_QPS_PERIOD = Integer.valueOf(get(KEY_SOA_MONITOR_QPS_PERIOD,"5"));
    public static Integer SOA_MONITOR_SERVICE_PROCESS_PERIOD = Integer.valueOf(get(KEY_SOA_MONITOR_SERVICE_PROCESS_PERIOD,"60"));
}
