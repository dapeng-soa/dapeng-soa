package com.github.dapeng.impl.filters.globalTrans;


import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.plugins.netty.MdcCtxInfoUtil;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.transaction.api.GlobalTransactionCallbackWithoutResult;
import com.github.dapeng.transaction.api.GlobalTransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by tangliu on 2016/4/11.
 */
public class SoaGlobalTransactionalFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaGlobalTransactionalFilter.class);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        long start = new Date().getTime();
        LOGGER.info("SoaGlobalTransactionalFilter startAt="+start);
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        Application application = (Application) ctx.getAttribute("application");

        try {

            SoaHeader soaHeader = (SoaHeader) ctx.getAttribute("soaHeader");
            SoaServiceDefinition serviceDef = (SoaServiceDefinition) ctx.getAttribute("serviceDef");
            if (soaHeader != null && serviceDef != null) {
                long count = new ArrayList<>(Arrays.asList(serviceDef.ifaceClass.getMethods()))
                        .stream()
                        .filter(m -> m.getName().equals(soaHeader.getMethodName()) && m.isAnnotationPresent(SoaGlobalTransactional.class))
                        .count();

                LOGGER.info("SoaGlobalTransactionalFilter count=" + count);
                if (count <= 0) {
                    for (Class<?> aClass : serviceDef.ifaceClass.getClass().getInterfaces()) {
                        count = count + new ArrayList<>(Arrays.asList(aClass.getMethods()))
                                .stream()
                                .filter(m -> m.getName().equals(soaHeader.getMethodName()) && m.isAnnotationPresent(SoaGlobalTransactional.class))
                                .count();

                        if (count > 0)
                            break;
                    }
                }
                final boolean isSoaGlobalTransactional = count > 0 ? true : false;
                LOGGER.info("SoaGlobalTransactionalFilter isSoaGlobalTransactional=" + isSoaGlobalTransactional);
                if (isSoaGlobalTransactional) {
                    context.setSoaGlobalTransactional(true);
                }
                if (soaHeader.getTransactionId().isPresent() || !SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {// in a global transaction
                    LOGGER.info("SoaGlobalTransactionalFilter start next.onEntry(ctx)1;");

                    next.onEntry(ctx);
                } else {

                    if (context.isSoaGlobalTransactional()) {

                        try {
                            new GlobalTransactionTemplate().execute(new GlobalTransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult() throws TException {
                                    LOGGER.info("SoaGlobalTransactionalFilter start next.onEntry2(ctx);");
                                    next.onEntry(ctx);
                                }
                            });
                        } catch (TException e) {
                            LOGGER.error(e.getMessage(), e);
                        }

                    } else {
                        LOGGER.info("SoaGlobalTransactionalFilter start next.onEntry3(ctx);");

                        next.onEntry(ctx);
                    }
                }
            }
        }finally {
            //remove current invocation
            InvocationContextImpl.Factory.removeCurrentInstance();
            try {
                next.onEntry(ctx);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                boolean isAsync = (Boolean) ctx.getAttribute("isAsync");
                if (isAsync) {
                    MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
                    MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);

                    MDC.remove(SoaSystemEnvProperties.THREAD_LEVEL_KEY);
                    MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.THREAD_LEVEL_KEY);
                }
            }
        }

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        prev.onExit(ctx);
    }

}