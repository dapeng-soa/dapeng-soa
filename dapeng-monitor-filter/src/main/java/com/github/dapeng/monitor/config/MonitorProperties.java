package com.github.dapeng.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * author with struy.
 * Create by 2018/2/1 19:07
 * email :yq1724555319@gmail.com
 */

public class MonitorProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorProperties.class);

    private static final Properties MONITOR_PROPERTIES = new Properties();
    private static final String KEY_MONITOR_INFLUXDB_DATABASE = "monitor.influxdb.database";
    private static final String KEY_MONITOR_QPS_PERIOD = "monitor.qps.period";
    private static final String KEY_MONITOR_SERVICE_PROCESS_PERIOD = "monitor.service.process.period";

    public static String MONITOR_INFLUXDB_DATABASE = get(KEY_MONITOR_INFLUXDB_DATABASE,"dapengState");
    public static Integer MONITOR_QPS_PERIOD = Integer.valueOf(get(KEY_MONITOR_QPS_PERIOD,"5"));
    public static Integer MONITOR_SERVICE_PROCESS_PERIOD = Integer.valueOf(get(KEY_MONITOR_SERVICE_PROCESS_PERIOD,"60"));

    private static String get(String key, String defaultValue) {
        try {
            InputStream in = MonitorProperties.class.getClassLoader().getResourceAsStream("config_monitor.properties");
            MONITOR_PROPERTIES.load(in);
        } catch (IOException e) {
            LOGGER.error("error reading file config_monitor.properties");
            LOGGER.error(e.getMessage(), e);
        }
        return MONITOR_PROPERTIES.getProperty(key, defaultValue);

    }
}
