package com.github.dapeng.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author hui
 * @date 2018/9/27 0027 19:48
 */
public class TtlExecutors {
    public static ExecutorService newSingleThreadExecutor() {
        return new TtlThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new TtlThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new TtlThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new TtlThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory);
    }
}
