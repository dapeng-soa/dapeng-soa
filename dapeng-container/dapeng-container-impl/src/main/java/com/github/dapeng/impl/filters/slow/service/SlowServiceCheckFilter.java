package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlowServiceCheckFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlowServiceCheckFilter.class);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {

        Task task = new Task(ctx);

        ctx.setAttach(this, "slowTimeCheckTask", task);

        next.onEntry(ctx);
    }


    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        Task task = (Task)ctx.getAttach(this,"slowTimeCheckTask");

        long processTime = System.currentTimeMillis() - task.startTime();
        LOGGER.info(" task startTime: " + task.startTime() + "  endTime: " + System.currentTimeMillis() + " processTime: " + processTime);

        long maxProcessTime = getMaxProccessTime(task.serviceName(),task.versionName(), task.methodName());

        if (processTime >= maxProcessTime) {
            final StackTraceElement[] stackElements = task.currentThread().getStackTrace();
            final StringBuilder builder = new StringBuilder(task.toString());
            builder.append(" ").append(processTime).append("ms");

            if (stackElements != null && stackElements.length > 0) {
                builder.append(" \n Slow Service StackTrace: ");
                for (int i = 0; i < stackElements.length; i++) {
                    builder.append("\n\tat " + stackElements[i]);
                }
            }

            LOGGER.info("SlowProcess:{}", builder.toString());
        }

        prev.onExit(ctx);
    }


    /**
     * 超时逻辑:
     * 1. TODO 如果Zk设置了服务最大处理时间，则取ZK的
     * 2. zk没有，则去环境变量的，否则取默认的最大处理时间
     *
     * @param service the serviceName to set
     * @param version the version to set
     * @param method the method to set
     * @return
     */
    private long getMaxProccessTime(String service, String version, String method) {

        long defaultTimeout = SoaSystemEnvProperties.SOA_MAX_PROCESS_TIME;

        return defaultTimeout;

    }

}
