package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;

public class SlowServiceCheckFilter implements Filter {

    private final TaskManager taskManager = new TaskManager();

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        System.out.println(" =============SlowServiceCheckFilter=============================");
        //初始化taskManager
        if (!taskManager.hasStarted()) {
            taskManager.start();
        }

        TransactionContext transactionContext = (TransactionContext) ctx.getAttribute("context");
        Task task = new Task(transactionContext);
        taskManager.addTask(task);

        ctx.setAttach(this, "slowTimeCheckTask", task);

        next.onEntry(ctx);
    }


    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        Task task = (Task)ctx.getAttach(this,"slowTimeCheckTask");

        taskManager.remove(task);

        prev.onExit(ctx);
    }
}
