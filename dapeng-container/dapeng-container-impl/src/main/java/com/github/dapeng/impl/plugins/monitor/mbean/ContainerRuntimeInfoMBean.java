package com.github.dapeng.impl.plugins.monitor.mbean;

/**
 * @author ever
 */
public interface ContainerRuntimeInfoMBean {
    //attribute？

    //op？

    /**
     * 停止容器
     * @return
     */
    public boolean stopContainer();

    /**
     * 禁用/开启监控
     * @param enable
     * @return
     */
    public boolean enableMonitor(boolean enable);

    /**
     * 获取日志级别
     * @return
     */
    public String getLoggerLevel();

    /**
     * 修改日志级别
     * @return
     */
    public String setLoggerLevel(String level);

    /**
     * 获取业务线程池情况
     * @return
     */
    public String getTheardPoolStatus();

    /**
     * 获取服务信息(包含容器信息)
     * @return
     */
    public String getSerivceBasicInfo();

}
