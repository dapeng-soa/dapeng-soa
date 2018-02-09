package com.github.dapeng.util;

/**
 * Soa System Env Properties
 *
 * @author craneding
 * @date 16/1/19
 */
public class SoaSystemEnvProperties {

    private static final String KEY_SOA_SERVICE_IP = "soa.service.ip";
    private static final String KEY_SOA_SERVICE_PORT = "soa.service.port";
    private static final String KEY_SOA_CONTAINER_IP = "soa.container.ip";
    private static final String KEY_SOA_CONTAINER_PORT = "soa.container.port";
    private static final String KEY_SOA_CALLER_IP = "soa.caller.ip";
    private static final String KEY_SOA_APIDOC_PORT = "soa.apidoc.port";
    private static final String KEY_SOA_KAFKA_HOST = "soa.kafka.host";
    private static final String KEY_SOA_BYTEBUF_ALLOCATOR = "soa.bytebuf.allocator";

    /**
     * 可指定主从竞选master
     */
    private static final String KEY_SOA_ZOOKEEPER_MASTER_HOST = "soa.zookeeper.master.host";
    private static final String KEY_SOA_ZOOKEEPER_HOST = "soa.zookeeper.host";
    //    private static final String KEY_SOA_ZOOKEEPER_REGISTRY_HOST = "soa.zookeeper.registry.host";
    private static final String KEY_SOA_ZOOKEEPER_FALLBACK_HOST = "soa.zookeeper.fallback.host";
    private static final String KEY_SOA_ZOOKEEPER_KAFKA_HOST = "soa.zookeeper.kafka.host";


    private static final String KEY_SOA_CONTAINER_USETHREADPOOL = "soa.container.usethreadpool";

    private static final String KEY_SOA_REMOTING_MODE = "soa.remoting.mode";
    private static final String KEY_SOA_MONITOR_ENABLE = "soa.monitor.enable";
    private static final String KEY_SOA_SERVICE_CALLERFROM = "soa.service.callerfrom";
    private static final String KEY_SOA_SERVICE_CLIENT_TIMEOUT = "soa.service.client.timeout";
    private static final String KEY_SOA_SERVICE_SERVER_TIMEOUT = "soa.service.server.timeout";
    private static final String KEY_SOA_CORE_POOL_SIZE = "soa.core.pool.size";
    private static final String KEY_SOA_MAX_READ_BUFFER_SIZE = "soa.max.read.buffer.size";
    private static final String KEY_SOA_LOCAL_HOST_NAME = "soa.local.host.name";
    private static final String KEY_SOA_TRANSACTIONAL_ENABLE = "soa.transactional.enable";

    private static final String KEY_SOA_FILTER_EXCLUDES = "soa.filter.excludes";
    private static final String KEY_SOA_FILTER_INCLUDES = "soa.filter.includes";

//    public static final String SOA_SERVICE_IP = get(KEY_SOA_SERVICE_IP, "127.0.0.1");
//    public static final boolean SOA_SERVICE_IP_ISCONFIG = get(KEY_SOA_SERVICE_IP) != null;
//
//    public static final Integer SOA_SERVICE_PORT = Integer.valueOf(get(KEY_SOA_SERVICE_PORT, "9090"));
    public static final String SOA_ZOOKEEPER_HOST = get(KEY_SOA_ZOOKEEPER_HOST, "127.0.0.1:2181");
    public static final boolean SOA_POOLED_BYTEBUF = get(KEY_SOA_BYTEBUF_ALLOCATOR, "pooled").equals("pooled");
    //    public static final String SOA_ZOOKEEPER_REGISTRY_HOST = get(KEY_SOA_ZOOKEEPER_REGISTRY_HOST, SOA_ZOOKEEPER_HOST);
    public static final String SOA_ZOOKEEPER_FALLBACK_HOST = get(KEY_SOA_ZOOKEEPER_FALLBACK_HOST, null);
    public static final boolean SOA_ZOOKEEPER_FALLBACK_ISCONFIG = get(KEY_SOA_ZOOKEEPER_FALLBACK_HOST) != null;
    public static final String SOA_ZOOKEEPER_MASTER_HOST = get(KEY_SOA_ZOOKEEPER_MASTER_HOST, null);
    public static final boolean SOA_ZOOKEEPER_MASTER_ISCONFIG = get(KEY_SOA_ZOOKEEPER_MASTER_HOST) != null;
    public static final String SOA_ZOOKEEPER_KAFKA_HOST = get(KEY_SOA_ZOOKEEPER_KAFKA_HOST, "127.0.0.1:2181");
    public static final String SOA_KAFKA_PORT = get(KEY_SOA_KAFKA_HOST,"127.0.0.1:9092");

    public static final boolean SOA_CONTAINER_USETHREADPOOL = Boolean.valueOf(get(KEY_SOA_CONTAINER_USETHREADPOOL, Boolean.TRUE.toString()));
    public static final String SOA_CONTAINER_IP = get(KEY_SOA_CONTAINER_IP, IPUtils.localIp());
    public static final String SOA_CALLER_IP = get(KEY_SOA_CALLER_IP, IPUtils.getCallerIp());
    public static final Integer SOA_CONTAINER_PORT = Integer.valueOf(get(KEY_SOA_CONTAINER_PORT, "9090"));
    public static final Integer SOA_APIDOC_PORT = Integer.valueOf(get(KEY_SOA_APIDOC_PORT, "8080"));
    public static final String SOA_REMOTING_MODE = get(KEY_SOA_REMOTING_MODE, "remote");
    public static final boolean SOA_MONITOR_ENABLE = Boolean.valueOf(get(KEY_SOA_MONITOR_ENABLE, "false"));
    public static final String SOA_SERVICE_CALLERFROM = get(KEY_SOA_SERVICE_CALLERFROM, "unknown");
    public static final Long SOA_SERVICE_CLIENT_TIMEOUT = Long.valueOf(get(KEY_SOA_SERVICE_CLIENT_TIMEOUT, "0"));
    public static final Long SOA_SERVICE_SERVER_TIMEOUT = Long.valueOf(get(KEY_SOA_SERVICE_SERVER_TIMEOUT, "0"));
    public static final long SOA_MAX_TIMEOUT = 60000L;
    public static final long SOA_DEFAULT_TIMEOUT = 500L;

    public static final Integer SOA_CORE_POOL_SIZE = Integer.valueOf(get(KEY_SOA_CORE_POOL_SIZE, (Runtime.getRuntime().availableProcessors() * 2) + ""));
    public static final Long SOA_MAX_READ_BUFFER_SIZE = Long.valueOf(get(KEY_SOA_MAX_READ_BUFFER_SIZE, (1024 * 1024 * 5) + ""));// 5M

    public static final String SOA_LOCAL_HOST_NAME = get(KEY_SOA_LOCAL_HOST_NAME);
    public static final boolean SOA_TRANSACTIONAL_ENABLE = Boolean.valueOf(get(KEY_SOA_TRANSACTIONAL_ENABLE, "true"));

    public static final String SOA_FILTER_EXCLUDES = get(KEY_SOA_FILTER_EXCLUDES,"");
    public static final String SOA_FILTER_INCLUDES = get(KEY_SOA_FILTER_INCLUDES,"");

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        String envValue = System.getenv(key.replaceAll("\\.", "_"));

        if (envValue == null)
            return System.getProperty(key, defaultValue);

        return envValue;
    }


    private static boolean isNotBlank(String val) {
        return val != null && !val.trim().isEmpty();
    }
}
