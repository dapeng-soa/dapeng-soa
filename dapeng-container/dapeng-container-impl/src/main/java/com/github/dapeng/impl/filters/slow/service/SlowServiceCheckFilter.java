package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlowServiceCheckFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger("container.slowtime.log");

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        if (SoaSystemEnvProperties.SOA_SLOW_SERVICE_CHECK_ENABLE) {
            SlowServiceCheckTask task = new SlowServiceCheckTask(ctx);
            ctx.setAttach(this, "slowServiceCheckTask", task);
            SlowServiceCheckTaskManager.addTask(task);
            if (!SlowServiceCheckTaskManager.hasStarted()) {
                //fixme lifecycle
                SlowServiceCheckTaskManager.start();
                logger.info("slow service check started");
            }
        }
        next.onEntry(ctx);
    }


    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        if (SoaSystemEnvProperties.SOA_SLOW_SERVICE_CHECK_ENABLE) {
            SlowServiceCheckTask task = (SlowServiceCheckTask) ctx.getAttach(this, "slowServiceCheckTask");
            SlowServiceCheckTaskManager.remove(task);
        }
        prev.onExit(ctx);
    }


}
