package com.github.dapeng.util;

import com.github.dapeng.core.TransactionContext;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author hui
 * @date 2018/9/27 0027 16:32
 */
public class TtlThreadPoolExecutor extends ThreadPoolExecutor {
    public TtlThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public TtlThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    @Override
    public void execute(Runnable runnable) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        TransactionContext transactionContext = TransactionContext.Factory.currentInstance();
        super.execute(() -> run(runnable, context, transactionContext));
    }

    private void run(Runnable runnable, Map<String, String> context, TransactionContext transactionContext) {
        MDC.setContextMap(context);
        TransactionContext.Factory.currentInstance(transactionContext);
        try {
            runnable.run();
        } finally {
            MDC.clear();
            TransactionContext.Factory.removeCurrentInstance();
        }
    }
}
