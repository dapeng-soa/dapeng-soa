package com.github.dapeng.impl.plugins.monitor.mbean;

/**
 * @author ever
 */
public interface ContainerRuntimeInfoMBean {
    //attribute？

    //op？

    // 停止容器
    public boolean stopContainer();

    // 禁用/开启监控
    public boolean enableMonitor(boolean enable);

    // 日志级别动态调整(logbak已实现)
    public String getLoggerLevel();

    // 获取线程池容量
    public Long getTheardPoolSize();

}
