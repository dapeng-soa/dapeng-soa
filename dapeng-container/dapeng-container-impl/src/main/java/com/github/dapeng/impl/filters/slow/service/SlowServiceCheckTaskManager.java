package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huyj
 * @Created 2018/6/26 14:14
 */
public class SlowServiceCheckTaskManager {

    private static final Logger logger = LoggerFactory.getLogger("container.slowtime.log");
    private static boolean live = false;
    private static List<SlowServiceCheckTask> tasks = Collections.synchronizedList(new ArrayList<>());
    private static Map<Thread, String> lastStackInfo = new ConcurrentHashMap<>();
    private static final long DEFAULT_SLEEP_TIME = 3000L;
    private static final long MAX_PROCESS_TIME = SoaSystemEnvProperties.SOA_MAX_PROCESS_TIME;

    public static void addTask(SlowServiceCheckTask task) {
        tasks.add(task);
    }

    public static void remove(SlowServiceCheckTask task) {
        lastStackInfo.remove(task.currentThread());
        tasks.remove(task);
    }

    public static boolean hasStarted() {
        return live;
    }

    public static void start() {
        live = true;
        final Thread targetThread = new Thread("slow-service-check-thread") {
            @Override
            public void run() {
                while (live) {
                    try {
                        checkSampleTask();
                        Thread.sleep(DEFAULT_SLEEP_TIME);
                    } catch (Exception e) {
                        logger.error("Check the task process time thread error", e);
                    }
                }
            }
        };
        targetThread.start();
    }

    public static void stop() {
        live = false;
        tasks.clear();
    }

    private static void checkSampleTask() throws InterruptedException {
        final List<SlowServiceCheckTask> tasksCopy = new ArrayList<>(tasks);
        logger.info("start check slow service  at [" + getCurrentTime() + "] Task queue " + tasksCopy.size());
        final Iterator<SlowServiceCheckTask> iterator = tasksCopy.iterator();

        while (iterator.hasNext()) {
            final long currentTime = System.currentTimeMillis();
            final SlowServiceCheckTask task = iterator.next();

            final long ptime = currentTime - task.startTime();
            if (ptime >= MAX_PROCESS_TIME) {
//            if (true) {
                final StackTraceElement[] stackElements = task.currentThread().getStackTrace();
                if (stackElements != null && stackElements.length > 0) {
                    final StringBuilder builder = new StringBuilder(task.toString());
                    builder.append("--[" + getCurrentTime() + "]:task info:[" + task.serviceName() + ":" + task.methodName() + ":" + task.versionName() + "]").append("\n");

                    final String firstStackInfo = stackElements[0].toString();
                    if (lastStackInfo.containsKey(task.currentThread()) && lastStackInfo.get(task.currentThread()).equals(firstStackInfo)) {
                        builder.append("Same as last check...");
                    } else {
                        builder.append("-- The task has been executed ").append(ptime).append("ms and Currently is executing:");
                        lastStackInfo.put(task.currentThread(), firstStackInfo);
                        builder.append("\n   at ").append(firstStackInfo.toString());
                        for (int i = 1; i < stackElements.length; i++) {
                            builder.append("\n   at " + stackElements[i]);
                        }
                    }
                    builder.append("\n").append("\n");
                    logger.error("SlowProcess:{}", builder.toString());
                }
            } else {
                lastStackInfo.remove(task.currentThread());
            }
        }
        tasksCopy.clear();
    }

    private static String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
    }
}
