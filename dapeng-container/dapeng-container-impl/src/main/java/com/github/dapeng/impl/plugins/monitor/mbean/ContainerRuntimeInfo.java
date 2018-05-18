package com.github.dapeng.impl.plugins.monitor.mbean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.dapeng.api.Container;
import com.github.dapeng.core.Application;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.github.dapeng.impl.plugins.netty.NettyConnectCounter;
import com.github.dapeng.impl.plugins.netty.SoaFlowCounter;
import com.github.dapeng.impl.plugins.netty.SoaInvokeCounter;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.ShmUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author with struy.
 * Create by 2018/3/22 14:44
 * email :yq1724555319@gmail.com
 * dapengMbean实现,必须和 Mbean的interface 同包
 */

public class ContainerRuntimeInfo implements ContainerRuntimeInfoMBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRuntimeInfo.class);
    private LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private final static String METHOD_NAME_KEY = "method_name";
    private String containerVersion = null;
    private final Container container;

    public ContainerRuntimeInfo(Container container) {
        super();
        this.container = container;
    }

    @Override
    public void setLoggerLevel(String loggerName, String levelStr) {
        if (loggerName == null) {
            return;
        }
        if (levelStr == null) {
            return;
        }
        loggerName = loggerName.trim();
        levelStr = levelStr.trim();

        LOGGER.info("Jmx Trying to set logger level [" + levelStr + "] to logger [" + loggerName +"]");

        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);
        if ("null".equalsIgnoreCase(levelStr)) {
            logger.setLevel(null);
        } else {
            Level level = Level.toLevel(levelStr, null);
            if (level != null) {
                logger.setLevel(level);
            }
        }
    }

    @Override
    public String getLoggerLevel(String loggerName) {
        if (loggerName == null) {
            return "";
        }

        loggerName = loggerName.trim();

        ch.qos.logback.classic.Logger logger = loggerContext.exists(loggerName);
        if (logger != null && logger.getLevel() != null) {
            return logger.getLevel().toString();
        } else {
            return "";
        }
    }

    @Override
    public boolean enableMonitor(boolean enable) {
        LOGGER.info("Jmx Switch Monitor to:", enable);
        MonitorFilterProperties.SOA_JMX_SWITCH_MONITOR = enable;
        return MonitorFilterProperties.SOA_JMX_SWITCH_MONITOR;
    }

    @Override
    public String getTheardPoolStatus() {
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) container.getDispatcher();
        return DumpUtil.dumpThreadPool(poolExecutor);
    }

    @Override
    public String getSerivceBasicInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nDapeng ContainerVersion ==> [ ")
                .append(getContainerVersion())
                .append(" ]\n");
        sb.append("\nCurrent Services Info ==> [ \n");
        for (Application application : container.getApplications()) {
            AtomicInteger count = new AtomicInteger();
            application.getServiceInfos().forEach(info -> {
                sb.append(count.incrementAndGet())
                        .append(". ")
                        .append(info)
                        .append("\n");
            });
        }
        sb.append("\n ]");
        return sb.toString();
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
    public String getNettyConnections() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nDapeng Netty Connections [ Active / Total / Inactive ] ==> [ ")
                .append(NettyConnectCounter.getActiveChannel())
                .append(" / ")
                .append(NettyConnectCounter.getTotalChannel())
                .append(" / ")
                .append(NettyConnectCounter.getInactiveChannel())
                .append(" ]");
        LOGGER.info(sb.toString());
        return sb.toString();
    }

    @Override
    public String getFreqControlCount(String app, String rule_type, int key) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("\nFreqControlCount data ==>")
                    .append(ShmUtil.freqControlCount(app,rule_type,key));
        } catch (Exception e) {
            LOGGER.error("getFreqControlCount error ::",e);
            return "getFreqControlCount error";
        }
        return sb.toString();
    }

    private String getContainerVersion() {
        if (null == containerVersion) {
            Properties properties = new Properties();
            try {
                properties.load(this.getClass().getClassLoader().getResourceAsStream("container.properties"));
                if (!properties.isEmpty()) {
                    containerVersion = properties.getProperty("container.version");
                }
            } catch (IOException e) {
                LOGGER.info("获取容器版本失败", e);
            }
        }
        return containerVersion;
    }
}
