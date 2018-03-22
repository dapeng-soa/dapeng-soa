package com.github.dapeng.impl.plugins.monitor;

import com.github.dapeng.impl.plugins.monitor.mbean.ContainerRuntimeInfoMBean;
import com.github.dapeng.util.SoaSystemEnvProperties;

/**
 * @author with struy.
 * Create by 2018/3/22 14:44
 * email :yq1724555319@gmail.com
 */

public class ContainerRuntimeInfo implements ContainerRuntimeInfoMBean {
    @Override
    public boolean stopContainer() {
        // TODO
        return false;
    }

    @Override
    public boolean enableMonitor(boolean enable) {
        // TODO
        return SoaSystemEnvProperties.SOA_MONITOR_ENABLE;
    }

    @Override
    public String getLoggerLevel() {
        // TODO
        return "DEBUG";
    }

    @Override
    public Long getTheardPoolSize() {
        // TODO
        return 0L;
    }
}
