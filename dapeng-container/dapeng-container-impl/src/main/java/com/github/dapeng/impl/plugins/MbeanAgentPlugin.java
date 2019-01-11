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
package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.impl.plugins.monitor.mbean.ContainerRuntimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * @author with struy.
 * Create by 2018/3/22 15:12
 * email :yq1724555319@gmail.com
 */

public class MbeanAgentPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(MbeanAgentPlugin.class);
    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private ObjectName mName = null;
    private final Container container;

    public MbeanAgentPlugin(Container container) {
        this.container = container;
    }

    @Override
    public void start() {
        try {
            mName = new ObjectName("com.github.dapeng:name=containerRuntimeInfo");
            //create mbean and register mbean
            ContainerRuntimeInfo runtimeInfo = new ContainerRuntimeInfo(container);
            server.registerMBean(runtimeInfo, mName);
            LOGGER.info("::registerMBean dapengContainerMBean success");
            LOGGER.info("::current service basicInfo: " + runtimeInfo.getServiceBasicInfo());
        } catch (Exception e) {
            LOGGER.info("::registerMBean dapengContainerMBean error [" + e.getMessage() + "]", e);
        }
    }

    @Override
    public void stop() {
        if (null != mName) {
            try {
                server.unregisterMBean(mName);
                LOGGER.info("::unregisterMBean dapengContainerMBean success");
            } catch (Exception e) {
                LOGGER.info("::unregisterMBean dapengContainerMBean error [" + e.getMessage() + "]", e);
            }
        }
    }


}
