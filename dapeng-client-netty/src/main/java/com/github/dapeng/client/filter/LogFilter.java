package com.github.dapeng.client.filter;


import com.github.dapeng.client.netty.SoaBaseConnection;
import com.github.dapeng.core.*;
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
    public void onEntry(FilterContext filterContext, FilterChain next) throws SoaException {
        try {
            InvocationContextImpl invocationContext = (InvocationContextImpl) filterContext.getAttribute("context");
            filterContext.setAttribute("startTime", System.currentTimeMillis());

            InvocationInfoImpl invocationInfo = new InvocationInfoImpl();
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
            next.onEntry(filterContext);
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) {
        try {
            InvocationContextImpl invocationContext = (InvocationContextImpl) filterContext.getAttribute("context");
            Long startTime = (Long) filterContext.getAttribute("startTime");
            InvocationInfoImpl invocationInfo = (InvocationInfoImpl) invocationContext.lastInvocationInfo();
            invocationInfo.serviceTime(System.currentTimeMillis() - startTime);

            SoaBaseConnection.Result result = (SoaBaseConnection.Result)filterContext.getAttribute("result");

            String respCode = "0000";
            if (result.exception != null) {
                respCode = result.exception.getCode();
            }

            String infoLog = "response[seqId:" + invocationContext.seqId() + ", respCode:" + respCode + ", server: " + filterContext.getAttribute("serverInfo") + "]: "
                    + "service[" + invocationContext.serviceName()
                    + "]: version[" + invocationContext.versionName()
                    + "]: method[" + invocationContext.methodName()
                    + "] cost[total: " + invocationInfo.serviceTime()
                    + ", calleeTime1: " + invocationInfo.calleeTime1()
                    + ", calleeTime2: " + invocationInfo.calleeTime2()
                    + ", calleeIp: " + invocationInfo.calleeIp();

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
