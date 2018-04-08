package com.github.dapeng.impl.plugins.monitor.mbean;

/**
 * @author ever
 */
public interface ContainerRuntimeInfoMBean {

    /**
     * 禁用/开启监控
     * @param enable
     * @return
     */
    boolean enableMonitor(boolean enable);

    /**
     * 获取日志级别
     * @return
     */
    String getLoggerLevel();

    /**
     * 修改日志级别
     * @return
     */
    String setLoggerLevel(String level);

    /**
     * 获取业务线程池情况
     * @return
     */
    String getTheardPoolStatus();

    /**
     * 获取服务信息(包含容器信息)
     * @return
     */
    String getSerivceBasicInfo();

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
    String getNettyChannelStatus();

}
