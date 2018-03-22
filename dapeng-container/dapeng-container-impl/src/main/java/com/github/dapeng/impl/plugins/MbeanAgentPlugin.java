package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Plugin;
import com.github.dapeng.impl.plugins.monitor.ContainerRuntimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * @author with struy.
 * Create by 2018/3/22 15:12
 * email :yq1724555319@gmail.com
 */

public class MbeanAgentPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(MbeanAgentPlugin.class);
    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private  ObjectName mName = null;

    @Override
    public void start() {
        try {
            mName = new ObjectName("dapengContainerMBean:name=containerRuntimeInfo");
            //create mbean and register mbean
            server.registerMBean(new ContainerRuntimeInfo(), mName);
            LOGGER.info("::registerMBean dapengContainerMBean success");
        } catch (Exception e) {
            LOGGER.info("::registerMBean dapengContainerMBean error [{}]",e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (null != mName){
            try {
                server.unregisterMBean(mName);
                LOGGER.info("::unregisterMBean dapengContainerMBean success");
            } catch (Exception e) {
                LOGGER.info("::unregisterMBean dapengContainerMBean error [{}]",e.getMessage());
            }
        }
    }
}
