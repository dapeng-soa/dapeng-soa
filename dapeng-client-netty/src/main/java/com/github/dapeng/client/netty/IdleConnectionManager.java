package com.github.dapeng.client.netty;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tangliu on 2016/2/2.
 */
public class IdleConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleConnectionManager.class);

    private boolean live = false;

    private static Map<Channel, AtomicInteger> channels = new ConcurrentHashMap<>();

    private static final long DEFAULT_SLEEP_TIME = 10000L;

    public static void addChannel(Channel channel) {

        if (!channels.containsKey(channel)) {
            AtomicInteger count = new AtomicInteger(1);
            channels.put(channel, count);
        } else {
            AtomicInteger count = channels.get(channel);
            count.incrementAndGet();
        }

        //channels.putIfAbsent(channel, new AtomicInteger(0)).incrementAndGet();
    }

    public static void remove(Channel channel) {
        channels.remove(channel);
    }

    public boolean hasStarted() {
        return live;
    }

    public void start() {
        live = true;

        final Thread targetThread = new Thread("Check idle connection Thread") {
            @Override
            public void run() {
                while (live) {
                    try {
                        checkIdleConnection();
                    } catch (Exception e) {
                        LOGGER.error("Check idle connection thread error", e);
                    }
                }
            }
        };
        targetThread.start();
    }

    public void stop() {
        live = false;
        channels.clear();
    }

    protected void checkIdleConnection() throws InterruptedException {
        Set<Channel> keys = channels.keySet();
        keys.stream().filter(channel -> channels.get(channel).get() > 10).forEach(channel -> {
            channel.close();
            remove(channel);

            LOGGER.info("channel:" + channel + " closed because of too much idle time");
        });
        //sleep, check per 10 seconds default
        Thread.sleep(DEFAULT_SLEEP_TIME);
    }
}
