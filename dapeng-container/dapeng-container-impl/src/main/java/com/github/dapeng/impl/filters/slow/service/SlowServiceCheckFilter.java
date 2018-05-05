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

        if (SoaSystemEnvProperties.SOA_SLOW_SERVICE_CHECK_ENABLE) {
            Task task = new Task(ctx);
            ctx.setAttach(this, "slowServiceCheckTask", task);
        }

        next.onEntry(ctx);
    }


    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        if (SoaSystemEnvProperties.SOA_SLOW_SERVICE_CHECK_ENABLE) {
            Task task = (Task)ctx.getAttach(this,"slowServiceCheckTask");

            long processTime = System.currentTimeMillis() - task.startTime();
            LOGGER.info(" task startTime: " + task.startTime() + "  endTime: " + System.currentTimeMillis() + " processTime: " + processTime);

            long maxProcessTime = (Long)ctx.getAttribute("slowServiceTime");

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
        }

        prev.onExit(ctx);
    }


}
