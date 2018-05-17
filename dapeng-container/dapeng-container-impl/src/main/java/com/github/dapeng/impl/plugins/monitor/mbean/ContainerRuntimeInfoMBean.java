package com.github.dapeng.impl.plugins.monitor.mbean;

/**
 * @author ever
 */
public interface ContainerRuntimeInfoMBean {

    /**
     * 设置日志级别
     * @param loggerName
     * @param levelStr
     */
    void setLoggerLevel(String loggerName, String levelStr);

    /**
     * 获取日志级别
     * @param loggerName
     * @return
     */
    String getLoggerLevel(String loggerName);

    /**
     * 禁用/开启监控
     * @param enable
     * @return
     */
    boolean enableMonitor(boolean enable);

    /**
     * 获取业务线程池情况
     * @return
     */
    String getThreadPoolStatus();

    /**
     * 获取服务信息(包含容器信息)
     * @return
     */
    String getServiceBasicInfo();

    /**
     * 获取当前运行服务流量信息
     * @return
     */
    String getServiceFlow();

    /**
     * 获取当前服务调用信息
     * @return
     */
    String getServiceInvoke();

    /**
     * 获取当前运行服务流量信息 (指定条数)
     * @param count 获取数据条数
     * @return
     */
    String getServiceFlow(int count);

    /**
     * 获取当前服务调用信息(指定条数)
     * @param count 获取的数据条数
     * @param methodName 方法名字
     * @return
     */
    String getServiceInvoke(int count, String methodName);

    /**
     * 获取Netty连接数信息
     * @return
     */
    String getNettyConnections();

}
