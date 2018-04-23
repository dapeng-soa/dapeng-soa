package com.github.dapeng.client.filter;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.InvocationInfoImpl;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author Ever
 * @date 2018-04-11
 */
public class LogFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) {
        try {
            Long startTime =System.currentTimeMillis();
            InvocationContextImpl invocationContext = (InvocationContextImpl) filterContext.getAttribute("context");
            InvocationInfoImpl invocationInfo = new InvocationInfoImpl();
            invocationInfo.serviceTime(startTime);
            invocationContext.lastInvocationInfo(invocationInfo);

            if (!invocationContext.sessionTid().isPresent()) {
                if (TransactionContext.hasCurrentInstance()
                        && TransactionContext.Factory.currentInstance().sessionTid().isPresent()) {
                    invocationContext.sessionTid(TransactionContext.Factory.currentInstance().sessionTid().get());
                } else {
                    invocationContext.sessionTid(DapengUtil.generateTid());
                }
            }

            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, invocationContext.sessionTid().orElse("0"));

            String infoLog = "request[seqId:" + invocationContext.seqId() + ", server:" + filterContext.getAttribute("serverInfo") + "]:"
                    + "service[" + invocationContext.serviceName()
                    + "]:version[" + invocationContext.versionName()
                    + "]:method[" + invocationContext.methodName() + "]";

            LOGGER.info(getClass().getSimpleName() + "::onEntry," + infoLog);
        } finally {
            try {
                next.onEntry(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) {
        try {
            InvocationContextImpl invocationContext = (InvocationContextImpl) filterContext.getAttribute("context");
            InvocationInfoImpl invocationInfo = (InvocationInfoImpl)invocationContext.lastInvocationInfo();
            long serviceTime = System.currentTimeMillis()-invocationInfo.serviceTime();
            invocationInfo.serviceTime(serviceTime);
            LOGGER.info("[lastInvocationInfo]:{0}",invocationInfo);

            String infoLog = "response[seqId:" + invocationContext.seqId() + ", server: " + filterContext.getAttribute("serverInfo") + "]:"
                    + "service[" + invocationContext.serviceName()
                    + "]:version[" + invocationContext.versionName()
                    + "]:method[" + invocationContext.methodName() + "]";

            LOGGER.info(getClass().getSimpleName() + "::onExit," + infoLog);
        } finally {
            try {
                prev.onExit(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            }
        }
    }
}
