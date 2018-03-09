package com.github.dapeng.client.filter;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.transaction.api.GlobalTransactionProcessTemplate;
import com.github.dapeng.util.SoaSystemEnvProperties;

/**
 * Created by tangliu on 2016/4/11.
 */
public class SoaTransactionalProcessFilter implements Filter {

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        final InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();
        boolean isSoaTransactionProcess = false;
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE && TransactionContext.hasCurrentInstance()
                && TransactionContext.Factory.getCurrentInstance().getCurrentTransactionId() > 0 && context.isSoaTransactionProcess()) {// in container and is a transaction process
            isSoaTransactionProcess = true;
        }
        ctx.setAttribute("isSoaTransactionProcess", isSoaTransactionProcess);
        next.onEntry(ctx);

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

        boolean isSoaTransactionProcess = (boolean) ctx.getAttribute("isSoaTransactionProcess");

        if (isSoaTransactionProcess) {
            Object req = ctx.getAttribute("request");
            try {
                new GlobalTransactionProcessTemplate<>(req).execute(() -> {
                    boolean isSuccess = ctx.getAttribute("isSuccess") == null ? true : (boolean) ctx.getAttribute("isSuccess");
                    return isSuccess;
                });
            } catch (TException e) {
                e.printStackTrace();
            }
        } else {
            prev.onExit(ctx);
        }

    }


}
