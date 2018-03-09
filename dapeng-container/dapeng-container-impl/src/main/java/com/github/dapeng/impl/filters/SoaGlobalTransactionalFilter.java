package com.github.dapeng.impl.filters;


import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaGlobalTransactional;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.ContainerFilter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.transaction.api.GlobalTransactionCallbackWithoutResult;
import com.github.dapeng.transaction.api.GlobalTransactionTemplate;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by tangliu on 2016/4/11.
 */
public class SoaGlobalTransactionalFilter implements ContainerFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaGlobalTransactionalFilter.class);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        TransactionContext context = TransactionContext.Factory.getCurrentInstance();

        SoaHeader soaHeader = (SoaHeader) ctx.getAttribute("soaHeader");
        SoaServiceDefinition serviceDef = (SoaServiceDefinition) ctx.getAttribute("serviceDef");
        System.out.println("~~~~~~soaheader:"+soaHeader+"~~~~~~serviceDef"+serviceDef.ifaceClass.getName());
        if (soaHeader != null && serviceDef != null) {
            System.out.println("~~~~~~~~~~~~~service name"+serviceDef.iface.getClass().getName());
            long count = new ArrayList<>(Arrays.asList(serviceDef.ifaceClass.getMethods()))
                    .stream()
                    .filter(m -> m.getName().equals(soaHeader.getMethodName()) && m.isAnnotationPresent(SoaGlobalTransactional.class))
                    .count();

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
            if (isSoaGlobalTransactional) {
                context.setSoaGlobalTransactional(true);
            }
            System.out.println("~~~~~~~~~~~count:"+count);
            if (soaHeader.getTransactionId().isPresent() || !SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {// in a global transaction
                System.out.println("~~~~~~~~~~~~~not global transaction");
                next.onEntry(ctx);
            } else {
                if (context.isSoaGlobalTransactional()) {

                    try {
                        new GlobalTransactionTemplate().execute(new GlobalTransactionCallbackWithoutResult() {
                            @Override
                            protected boolean doInTransactionWithResult() throws TException {
                                next.onEntry(ctx);
                                Object isSuccess =  ctx.getAttribute("isSuccess");
                                if (isSuccess != null){
                                    return (boolean) isSuccess;
                                }else{
                                    return  true;
                                }
                            }
                        });
                    } catch (TException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                } else {
                    next.onEntry(ctx);
                }
            }
        }

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        prev.onExit(ctx);
    }

}
