package com.github.dapeng.core.helper;

/**
 * Soa System Env Properties
 *
 * @author craneding
 * @date 16/1/19
 */
public class SoaSystemEnvProperties {

    public static final long SOA_MAX_TIMEOUT = 300000L;
    public static final long SOA_DEFAULT_TIMEOUT = 1000L;

    /**
     * 容器IP
     */
    private static final String KEY_SOA_CONTAINER_IP = "soa.container.ip";
    /**
     * 容器端口(默认9090)
     */
    private static final String KEY_SOA_CONTAINER_PORT = "soa.container.port";
    /**
     * kafka host(默认127.0.0.1:9092)
     */
    private static final String KEY_SOA_KAFKA_HOST = "soa.kafka.host";
    /**
     * Netty ByteBuf ALLOCATOR 对象的创建方式(默认pooled)
     */
    private static final String KEY_SOA_BYTEBUF_ALLOCATOR = "soa.bytebuf.allocator";

    public static final String KEY_LOGGER_SESSION_TID = "sessionTid";
    /**
     * 可指定主从竞选master (需要时配置IP:2181)
     */
    private static final String KEY_SOA_ZOOKEEPER_MASTER_HOST = "soa.zookeeper.master.host";
    /**
     * 注册中心 (默默127.0.0.1:2181)
     */
    private static final String KEY_SOA_ZOOKEEPER_HOST = "soa.zookeeper.host";
    /**
     *是否使用 灰度 zk (需要时配置IP:2181)
     */
    private static final String KEY_SOA_ZOOKEEPER_FALLBACK_HOST = "soa.zookeeper.fallback.host";
    /**
     * 文档站点端口(默认8080)
     */
    private static final String KEY_SOA_APIDOC_PORT = "soa.apidoc.port";

    /**
     * 是否使用业务线程池开关(默认true)
     */
    private static final String KEY_SOA_CONTAINER_USETHREADPOOL = "soa.container.usethreadpool";
    /**
     * 服务限流开关(默认false)
     */
    private static final String KEY_SOA_FREQ_LIMIT_ENABLE = "soa.freq.limit.enable";

    /**
     * 限流数据共享内存文件（默认是/data/shm.data）
     */
    private static final String KEY_SOA_FREQ_SHM_DATA = "soa.freq.shm.data";
    /**
     * 服务监控开关 (默认false)
     */
    private static final String KEY_SOA_MONITOR_ENABLE = "soa.monitor.enable";
    /**
     * 服务超时时间(默认0)
     */
    private static final String KEY_SOA_SERVICE_TIMEOUT = "soa.service.timeout";

    /**
     * 业务线程池大小(默认为服务器处理器数*2)
     */
    private static final String KEY_SOA_CORE_POOL_SIZE = "soa.core.pool.size";
    /**
     * 请求缓冲区大小(默认5M)
     */
    private static final String KEY_SOA_MAX_READ_BUFFER_SIZE = "soa.max.read.buffer.size";
    /**
     * 设置本地主机名称
     */
    private static final String KEY_SOA_LOCAL_HOST_NAME = "soa.local.host.name";
    /**
     * 全局事务 开关(默认true)
     */
    private static final String KEY_SOA_TRANSACTIONAL_ENABLE = "soa.transactional.enable";

    /**
     * 排除不需要的Filter
     */
    private static final String KEY_SOA_FILTER_EXCLUDES = "soa.filter.excludes";
    /**
     * 加载需要的Filter
     */
    private static final String KEY_SOA_FILTER_INCLUDES = "soa.filter.includes";

     /**
      * subPool 连接数， 单个客户端跟单个服务节点之间的连接数， 默认是1
      */
    private static final String KEY_SOA_SUBPOOL_SIZE = "soa.subPool.size";


    /**
     * 服务实例权重(默认100)
     */
    private static final String KEY_SOA_INSTANCE_WEIGHT = "soa.instance.weight";

    /**
     * 默认最大处理时间， 超过即认为是慢服务（默认3000）
     */
    private static final String KEY_SOA_MAX_PROCESS_TIME = "soa.max.process.time";
    /**
     * 慢服务检查开关(默认true)
     */
    private static final String KEY_SOA_SLOW_SERVICE_CHECK_ENABLE = "slow.service.check.enable";

    /**
     * 数据库用户信息加密开关(默认false)
     */
    private static final String KEY_SOA_DATABASE_ENCRYPT_ENABLE = "database.encrypt.enable";

    /**
     * 数据库用户信息加密  公钥
     */
    private static final String KEY_SOA_DATABASE_ENCRYPT_PUBLICKEY = "database.encrypt.publickey";



    public static final String SOA_ZOOKEEPER_HOST = get(KEY_SOA_ZOOKEEPER_HOST, "127.0.0.1:2181");
    public static final boolean SOA_POOLED_BYTEBUF = get(KEY_SOA_BYTEBUF_ALLOCATOR, "pooled").equals("pooled");

    // zk fallback zk
    public static final String SOA_ZOOKEEPER_FALLBACK_HOST = get(KEY_SOA_ZOOKEEPER_FALLBACK_HOST, null);
    public static final boolean SOA_ZOOKEEPER_FALLBACK_ISCONFIG = get(KEY_SOA_ZOOKEEPER_FALLBACK_HOST) != null;
    public static final String SOA_ZOOKEEPER_MASTER_HOST = get(KEY_SOA_ZOOKEEPER_MASTER_HOST, null);
    public static final boolean SOA_ZOOKEEPER_MASTER_ISCONFIG = get(KEY_SOA_ZOOKEEPER_MASTER_HOST) != null;
    public static final String SOA_KAFKA_HOST = get(KEY_SOA_KAFKA_HOST, "127.0.0.1:9092");

    public static final boolean SOA_CONTAINER_USETHREADPOOL = Boolean.valueOf(get(KEY_SOA_CONTAINER_USETHREADPOOL, Boolean.TRUE.toString()));
    public static final String SOA_CONTAINER_IP = get(KEY_SOA_CONTAINER_IP, IPUtils.containerIp());
    public static final int SOA_CONTAINER_PORT = Integer.valueOf(get(KEY_SOA_CONTAINER_PORT, "9090"));
    public static final int SOA_APIDOC_PORT = Integer.valueOf(get(KEY_SOA_APIDOC_PORT, "8080"));
    public static final boolean SOA_MONITOR_ENABLE = Boolean.valueOf(get(KEY_SOA_MONITOR_ENABLE, "false"));
    public static final boolean SOA_FREQ_LIMIT_ENABLE = Boolean.valueOf(get(KEY_SOA_FREQ_LIMIT_ENABLE, "false"));
    public static final String SOA_FREQ_SHM_DATA = get(KEY_SOA_FREQ_SHM_DATA, "/data/shm.data");
    public static final long SOA_SERVICE_TIMEOUT = Long.valueOf(get(KEY_SOA_SERVICE_TIMEOUT, "0"));

    public static final int SOA_CORE_POOL_SIZE = Integer.valueOf(get(KEY_SOA_CORE_POOL_SIZE, String.valueOf(Runtime.getRuntime().availableProcessors() * 2)));
    public static final long SOA_MAX_READ_BUFFER_SIZE = Long.valueOf(get(KEY_SOA_MAX_READ_BUFFER_SIZE, String.valueOf(1024 * 1024 * 5)));// 5M

    public static final String SOA_LOCAL_HOST_NAME = get(KEY_SOA_LOCAL_HOST_NAME);
    public static final boolean SOA_TRANSACTIONAL_ENABLE = Boolean.valueOf(get(KEY_SOA_TRANSACTIONAL_ENABLE, "true"));

    public static final String SOA_FILTER_EXCLUDES = get(KEY_SOA_FILTER_EXCLUDES, "");
    public static final String SOA_FILTER_INCLUDES = get(KEY_SOA_FILTER_INCLUDES, "");

    public static final int SOA_SUBPOOL_SIZE = Integer.valueOf(get(KEY_SOA_SUBPOOL_SIZE,"1"));
    /**
     * 默认服务处理最大时间为10s, 超过即认为是慢服务
     */
    public static final long SOA_MAX_PROCESS_TIME = Long.valueOf(get(KEY_SOA_MAX_PROCESS_TIME, "3000"));
    public static final boolean SOA_SLOW_SERVICE_CHECK_ENABLE = Boolean.valueOf(get(KEY_SOA_SLOW_SERVICE_CHECK_ENABLE, "true"));


    /*
    * 数据库加密配置
    * */
    public static final String SOA_DATABASE_ENCRYPT_PUBLICKEY = get(KEY_SOA_DATABASE_ENCRYPT_PUBLICKEY, null);
    public static final boolean SOA_DATABASE_ENCRYPT_ENABLE = Boolean.valueOf(get(KEY_SOA_DATABASE_ENCRYPT_ENABLE, "false"));

    /**
     * 正常返回的时候的response code
     */
    public static final String SOA_NORMAL_RESP_CODE = "0000";

    /**
     * 默认的服务实例的权重
     */
    public static final Integer SOA_INSTANCE_WEIGHT = Integer.valueOf(get(KEY_SOA_INSTANCE_WEIGHT, "100"));



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




    /*---暂时未使用的环境变量名称-------------------------------------*/
    private static final String KEY_SOA_SERVICE_IP = "soa.service.ip";
    private static final String KEY_SOA_SERVICE_PORT = "soa.service.port";
    private static final String KEY_SOA_ZOOKEEPER_KAFKA_HOST = "soa.zookeeper.kafka.host";
    private static final String KEY_SOA_CALLER_IP = "soa.caller.ip";
    private static final String KEY_SOA_ZOOKEEPER_REGISTRY_HOST = "soa.zookeeper.registry.host";
    private static final String KEY_SOA_EVENT_MESSAGE_TOPIC = "soa.event.topic";
    /**
     * 消息总线 定时间隔
     */
    private static final String KEY_SOA_EVENTBUS_PERIOD = "soa.eventbus.publish.period";
    private static final String KEY_SOA_SERVICE_CALLERFROM = "soa.service.callerfrom";
    private static final String KEY_SOA_REMOTING_MODE = "soa.remoting.mode";


    /*---暂时未使用的环境变量值-------------------------------------*/
    public static final String SOA_ZOOKEEPER_REGISTRY_HOST = get(KEY_SOA_ZOOKEEPER_REGISTRY_HOST, SOA_ZOOKEEPER_HOST);
    public static final String SOA_ZOOKEEPER_KAFKA_HOST = get(KEY_SOA_ZOOKEEPER_KAFKA_HOST, "127.0.0.1:2181");
    public static final String SOA_CALLER_IP = get(KEY_SOA_CALLER_IP, IPUtils.localIp());
    public static final String SOA_REMOTING_MODE = get(KEY_SOA_REMOTING_MODE, "remote");
    public static final String SOA_SERVICE_CALLERFROM = get(KEY_SOA_SERVICE_CALLERFROM, "unknown");
    public static final String SOA_EVENT_MESSAGE_TOPIC = get(KEY_SOA_EVENT_MESSAGE_TOPIC, "");
    public static final String SOA_EVENTBUS_PERIOD = get(KEY_SOA_EVENTBUS_PERIOD, "1000");

}
