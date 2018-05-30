package com.github.dapeng.common;

/**
 * 配置  信息
 *
 * @author huyj
 * @Created 2018/5/24 15:39
 */
public class BaseConfig {

    /*重试策略，初试时间1秒，重试10次*/
    static final int RETRY_TIME = 1000;
    static final int RETRY_NUM = 10;

    /*客户端连接配置 */
    static final String CONNECT_ADDR = "127.0.0.1:2181";
    static final int SESSION_TIMEOUT_MS = 60000; //会话超时时间，单位毫秒，默认60000ms
    static final int CONNECTION_TIMEOUT_MS = 500; //连接创建超时时间，单位毫秒，默认60000ms
    static final String NAMESPACE = "soa";

    /*数据节点 路径*/
    public final static String ZK_ROOT_PATH = "/soa";
    public final static String RUNTIME_PATH = "/soa/runtime/services";
    public final static String CONFIG_PATH = "/soa/config/services";
    public final static String ROUTES_PATH = "/soa/config/routes";
    public final static String FREQ_PATH = "/soa/config/freq";
    public static final String WHITELIST_PATH = "/soa/whitelist/services";

    public final static String MONITOR_RUNTIME_PATH = RUNTIME_PATH + "/";
    public final static String MONITOR_ROUTES_PATH = ROUTES_PATH + "/";
    public final static String MONITOR_FREQ_PATH = FREQ_PATH + "/";
    public static final String MONITOR_WHITELIST_PATH = WHITELIST_PATH + "/";

}
