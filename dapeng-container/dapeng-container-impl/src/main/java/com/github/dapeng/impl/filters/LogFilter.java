package com.github.dapeng.impl.filters;


import com.github.dapeng.core.Application;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.plugins.netty.MdcCtxInfoUtil;
import com.github.dapeng.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ever
 * @date 2018-04-11
 */
public class LogFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) {
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        Application application = (Application) filterContext.getAttribute("application");


        try {
            // 容器的IO线程MDC以及应用的MDC(不同classLoader)设置
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));
            MdcCtxInfoUtil.switchMdcToAppClassLoader("put", application.getAppClasssLoader(), transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(getClass().getSimpleName() + "::onEntry[seqId:" + transactionContext.seqId() + "]");
            }

            SoaHeader soaHeader = transactionContext.getHeader();

            String infoLog = "request[seqId:" + transactionContext.seqId() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "") + " "
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "") + " "
                    + (soaHeader.getUserIp().isPresent() ? " userIp:" + IPUtils.transferIp(soaHeader.getUserIp().get()) : "");


            application.info(this.getClass(), infoLog);
        } finally {
            try {
                next.onEntry(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                boolean isAsync = (Boolean) filterContext.getAttribute("isAsync");
                if (isAsync) {
                    MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
                    MdcCtxInfoUtil.switchMdcToAppClassLoader("remove", application.getAppClasssLoader(), null);
                }
            }
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) {
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        Application application = (Application) filterContext.getAttribute("application");

        boolean isAsync = (Boolean) filterContext.getAttribute("isAsync");

        try {
            if (isAsync) {
                MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));
                MdcCtxInfoUtil.switchMdcToAppClassLoader("put", application.getAppClasssLoader(), transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(getClass().getSimpleName()
                        + "::onExit:[seqId:" + transactionContext.seqId()
                        + ", execption:" + transactionContext.soaException()
                        + ",\n result:" + filterContext.getAttribute("result") + "]\n");
            }

            SoaHeader soaHeader = transactionContext.getHeader();

            Long requestTimestamp = (Long) transactionContext.getAttribute("dapeng_request_timestamp");

            Long cost = System.currentTimeMillis() - requestTimestamp;
            String infoLog = "response[seqId:" + transactionContext.seqId() + ", respCode:" + soaHeader.getRespCode().get() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "")
                    + " cost:" + cost + "ms";
            soaHeader.setCalleeTime1(cost.intValue());
            application.info(this.getClass(), infoLog);
        } finally {
            try {
                prev.onExit(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                MdcCtxInfoUtil.switchMdcToAppClassLoader("remove", application.getAppClasssLoader(), null);
                MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            }
        }
    }
}
