/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.impl.plugins.monitor.mbean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.dapeng.api.Container;
import com.github.dapeng.core.Application;
import com.github.dapeng.impl.plugins.monitor.ServerCounterContainer;
import com.github.dapeng.impl.plugins.monitor.config.MonitorFilterProperties;
import com.github.dapeng.util.DumpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dapeng.core.helper.DapengUtil.CONTAINER_VERSION;

/**
 * @author with struy.
 * Create by 2018/3/22 14:44
 * email :yq1724555319@gmail.com
 * dapengMbean实现,必须和 Mbean的interface 同包
 */

public class ContainerRuntimeInfo implements ContainerRuntimeInfoMBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRuntimeInfo.class);
    private LoggerContext loggerContext = null;
    private final static String METHOD_NAME_KEY = "method_name";
    private final static ServerCounterContainer counterContainer = ServerCounterContainer.getInstance();
    private final Container container;

    public ContainerRuntimeInfo(Container container) {
        super();
        this.container = container;
        try {
            loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        }catch (Exception e){
            LOGGER.info("loggerContext get error",e);
        }
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
    public String getThreadPoolStatus() {
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) container.getDispatcher();
        StringBuilder sb = new StringBuilder();
        sb.append("[Dapeng Mbean] Dapeng TheardPoolStatus == ");
        sb.append(DumpUtil.dumpThreadPool(poolExecutor));
        return sb.toString();
    }

    @Override
    public String getServiceBasicInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Dapeng Mbean] Dapeng ContainerVersion == [ ")
                .append(getContainerVersion())
                .append(" ]");
        sb.append("\nCurrent Services Info == [ \n");
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
//        StringBuilder sb = new StringBuilder();
//        sb.append("\nServiceFlow data == ");
//        SoaFlowCounter.getFlowCacheQueue().forEach(x -> {
//            sb.append("\n").append(x.toString()).append("\n");
//        });
//        return sb.toString();
        return "\nNot implemented yet";
    }

    @Override
    public String getServiceInvoke() {

//        StringBuilder sb = new StringBuilder();
//        sb.append("\nServiceInvoke data == ");
//        SoaInvokeCounter.getServiceCacheQueue().forEach(x -> {
//            sb.append("\n");
//            x.forEach(y -> sb.append(y.toString()));
//            sb.append("\n");
//        });
//        return sb.toString();
        return "\nNot implemented yet";
    }

    @Override
    public String getServiceFlow(int count) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("\nServiceFlow data count ==> [ ")
//                .append(SoaFlowCounter.getFlowCacheQueue().size())
//                .append("/")
//                .append(count).append(" ]");
//        SoaFlowCounter.getFlowCacheQueue().forEach(x -> {
//            sb.append("\n").append(x.toString()).append("\n");
//        });
//        return sb.toString();
        return "\nNot implemented yet";
    }

    @Override
    public String getServiceInvoke(int count, String methodName) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("\nServiceInvoke data count ==> [ ")
//                .append(SoaInvokeCounter.getServiceCacheQueue().size())
//                .append("/")
//                .append(count).append(" ]");
//        SoaInvokeCounter.getServiceCacheQueue().forEach(x -> {
//            sb.append("\n");
//            x.forEach(y -> {
//                if (methodName.equals(y.getTags().get(METHOD_NAME_KEY))) {
//                    sb.append(y.toString());
//                }
//            });
//            sb.append("\n");
//        });
//        return sb.toString();
        return "\nNot implemented yet";
    }

    @Override
    public String getNettyConnections() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Dapeng Mbean] Dapeng Netty Connections == [ Active/Inactive/Total ] == [ ")
                .append(counterContainer.getActiveChannel())
                .append("/")
                .append(counterContainer.getInactiveChannel())
                .append("/")
                .append(counterContainer.getTotalChannel())
                .append(" ]");
        LOGGER.info(sb.toString());
        return sb.toString();
    }

    private String getContainerVersion() {
        return CONTAINER_VERSION;
    }
}
