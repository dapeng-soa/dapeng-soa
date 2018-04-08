package com.github.dapeng.impl.plugins.monitor.mbean;

import com.github.dapeng.api.Container;
import com.github.dapeng.impl.plugins.netty.NettyConnectCounter;
import com.github.dapeng.impl.plugins.netty.SoaFlowCounter;
import com.github.dapeng.impl.plugins.netty.SoaInvokeCounter;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.SoaSystemEnvProperties;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author with struy.
 * Create by 2018/3/22 14:44
 * email :yq1724555319@gmail.com
 * dapengMbean实现,必须和 Mbean的interface 同包
 */

public class ContainerRuntimeInfo implements ContainerRuntimeInfoMBean {
    private final static String METHOD_NAME_KEY = "method_name";
    private final Container container;

    public ContainerRuntimeInfo(Container container) {
        super();
        this.container = container;
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
        // TODO
        return level;
    }

    @Override
    public String getTheardPoolStatus() {
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) container.getDispatcher();
        return DumpUtil.dumpThreadPool(poolExecutor);
    }

    @Override
    public String getSerivceBasicInfo() {
        // TODO
        return null;
    }

    @Override
    public String getServiceFlow() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nServiceFlow data ==> ");
        SoaFlowCounter.getFlowCacheQueue().forEach(x -> {
            sb.append("\n").append(x.toString()).append("\n");
        });
        return sb.toString();
    }

    @Override
    public String getServiceInvoke() {

        StringBuilder sb = new StringBuilder();
        sb.append("\nServiceInvoke data ==> ");
        SoaInvokeCounter.getServiceCacheQueue().forEach(x -> {
            sb.append("\n");
            x.forEach(y -> sb.append(y.toString()));
            sb.append("\n");
        });
        return sb.toString();
    }

    @Override
    public String getServiceFlow(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nServiceFlow data count ==> [ ")
                .append(SoaFlowCounter.getFlowCacheQueue().size())
                .append("/")
                .append(count).append(" ]");
        SoaFlowCounter.getFlowCacheQueue().forEach(x -> {
            sb.append("\n").append(x.toString()).append("\n");
        });
        return sb.toString();
    }

    @Override
    public String getServiceInvoke(int count, String methodName) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nServiceInvoke data count ==> [ ")
                .append(SoaInvokeCounter.getServiceCacheQueue().size())
                .append("/")
                .append(count).append(" ]");
        SoaInvokeCounter.getServiceCacheQueue().forEach(x -> {
            sb.append("\n");
            x.forEach(y -> {
                if (methodName.equals(y.getTags().get(METHOD_NAME_KEY))) {
                    sb.append(y.toString());
                }
            });
            sb.append("\n");
        });
        return sb.toString();
    }

    @Override
    public String getNettyChannelStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nDapeng Netty Connections [ Active / Total /Inactive ] ==> [ ")
                .append(NettyConnectCounter.getActiveChannel())
                .append(" / ")
                .append(NettyConnectCounter.getTotalChannel())
                .append(" / ")
                .append(NettyConnectCounter.getInactiveChannel())
                .append(" ]");
        return sb.toString();
    }
}
