package com.github.dapeng.impl.plugins.monitor.mbean;

import com.github.dapeng.api.Container;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.SoaSystemEnvProperties;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author with struy.
 * Create by 2018/3/22 14:44
 * email :yq1724555319@gmail.com
 * 实现必须和 interface 同包
 */

public class ContainerRuntimeInfo implements ContainerRuntimeInfoMBean {
    private final Container container;
    public ContainerRuntimeInfo(Container container){
        super();
        this.container = container;
    }
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
    public String setLoggerLevel(String level) {
        return level;
    }

    @Override
    public String getTheardPoolStatus() {
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) container.getDispatcher();
        return DumpUtil.dumpThreadPool(poolExecutor);
    }

    @Override
    public String getSerivceBasicInfo() {
        return null;
    }
}
