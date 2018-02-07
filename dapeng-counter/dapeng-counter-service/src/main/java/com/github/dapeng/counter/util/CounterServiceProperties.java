package com.github.dapeng.counter.util;

import com.github.dapeng.util.SoaSystemEnvProperties;

/**
 * author with struy.
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
