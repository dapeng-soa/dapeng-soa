package com.github.dapeng.impl.filters;


import com.github.dapeng.core.Application;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.ContainerFilter;
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

/**
 * @author Ever
 * @date 2018-04-11
 */
public class LogFilter implements ContainerFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);
    private static final boolean logFormatEnable = SoaSystemEnvProperties.SOA_LOG_FORMAT_ENABLE;

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) {
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        Application application = (Application) filterContext.getAttribute("application");
        Object args = filterContext.getAttribute("args");


        try {
            // 容器的IO线程MDC以及应用的MDC(不同classLoader)设置
            String sessionTid = transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0");
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
            MdcCtxInfoUtil.putMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

            SoaHeader soaHeader = transactionContext.getHeader();
            String logLevel = soaHeader.getCookie(SoaSystemEnvProperties.THREAD_LEVEL_KEY);

            if (logLevel != null) {
                MDC.put(SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
                MdcCtxInfoUtil.putMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(getClass().getSimpleName() + "::onEntry[seqId:" + transactionContext.seqId() + "]");
            }

            SoaHeader soaHeader = transactionContext.getHeader();

            String infoLog = "request[seqId:" + transactionContext.seqId() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "") + " "
                    + (soaHeader.getOperatorName().isPresent() ? " operatorName:" + soaHeader.getOperatorName().get() : "") + " "
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "") + " "
                    + (soaHeader.getUserIp().isPresent() ? " userIp:" + IPUtils.transferIp(soaHeader.getUserIp().get()) + " "
                    + (args != null ? " args:[" + (logFormatEnable ? formatToString(args.toString()) :args.toString()) + "]" : "");



            application.info(this.getClass(), infoLog);
        } finally {
            //remove current invocation
            InvocationContextImpl.Factory.removeCurrentInstance();
            try {
                next.onEntry(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                boolean isAsync = (Boolean) filterContext.getAttribute("isAsync");
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
    public void onExit(FilterContext filterContext, FilterChain prev) {
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        Application application = (Application) filterContext.getAttribute("application");
        Object result = filterContext.getAttribute("result");

        boolean isAsync = (Boolean) filterContext.getAttribute("isAsync");

        try {
            if (isAsync) {
                try {
                    String sessionTid = transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0");
                    MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
                    MdcCtxInfoUtil.putMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);

                    //DEBUG
                    SoaHeader soaHeader = transactionContext.getHeader();
                    String logLevel = soaHeader.getCookie(SoaSystemEnvProperties.THREAD_LEVEL_KEY);

                    if (logLevel != null) {
                        MDC.put(SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
                        MdcCtxInfoUtil.putMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.THREAD_LEVEL_KEY, logLevel);
                    }
                } finally {
                    //remove current invocation
                    InvocationContextImpl.Factory.removeCurrentInstance();
                }
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
                    + (soaHeader.getOperatorName().isPresent() ? " operatorName:" + soaHeader.getOperatorName().get() : "")
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : ""
                    + (result != null ? " result:[" + (logFormatEnable ? formatToString(result.toString()) : result.toString()) + "]" : "")
                    + " cost:" + cost + "ms");
            soaHeader.setCalleeTime1(cost.intValue());
            application.info(this.getClass(), infoLog);
        } finally {
            try {
                prev.onExit(filterContext);
            } catch (TException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
                MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);

                MDC.remove(SoaSystemEnvProperties.THREAD_LEVEL_KEY);
                MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.THREAD_LEVEL_KEY);

            }
        }


    }

    private static String formatToString(String msg) {
        if (msg == null)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }
}
