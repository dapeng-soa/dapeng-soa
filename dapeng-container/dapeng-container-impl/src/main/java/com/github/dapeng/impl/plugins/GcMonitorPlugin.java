package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.sun.management.GarbageCollectionNotificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.function.Consumer;

import static com.github.dapeng.core.helper.SoaSystemEnvProperties.SOA_GC_RADAR_ALERT_LEVEL;

/**
 * Gc monitor for handling FullGc
 */
public class GcMonitorPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcMonitorPlugin.class);
    private Container container;
    private final List<GarbageCollectorMXBean> gcbeans;
    private final int memWaterMark = SOA_GC_RADAR_ALERT_LEVEL;

    public GcMonitorPlugin(Container container) {
        this.container = container;
        gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
    }

    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");
        //Install a notifcation handler for each bean
        for (GarbageCollectorMXBean gcbean : gcbeans) {
            NotificationEmitter emitter = (NotificationEmitter) gcbean;

            //Add the listener, we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
            emitter.addNotificationListener(listener,
                    (notification) -> notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION),
                    gcNotification);
        }
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::started");
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
        for(GarbageCollectorMXBean mBean: gcbeans) {
            try {
                ((NotificationEmitter) mBean).removeNotificationListener(listener);
            } catch(ListenerNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stopped");
    }

    private NotificationListener listener = (notification, handback) -> ((Consumer<Notification>)handback).accept(notification);

    private Consumer<Notification> gcNotification = notification -> {
        //we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
        assert notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION);
        //get the information associated with this notification
        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        String gctype = info.getGcAction();
        if ("end of minor GC".equals(gctype)) {
            gctype = "Young Gen GC";
        } else if ("end of major GC".equals(gctype)) {
            gctype = "Old Gen GC";
        }
        //Get the information about each memory space, and pretty print it
        MemoryUsage membefore = info.getGcInfo().getMemoryUsageAfterGc().get("PS Old Gen");
        long memCommitted = membefore.getCommitted();
        long memUsed = membefore.getUsed();
        long percent = (memUsed * 100L) / memCommitted;
        if (percent >= memWaterMark) {
            if (container.status() == Container.STATUS_RUNNING) {
                container.pause();
            }
        } else if (container.status() == Container.STATUS_PAUSE) {
            container.resume();
        }
    };
}
