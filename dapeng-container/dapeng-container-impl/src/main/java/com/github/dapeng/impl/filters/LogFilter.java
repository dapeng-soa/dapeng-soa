package com.github.dapeng.impl.filters;


import com.github.dapeng.core.Application;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.plugins.netty.NettyChannelKeys;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ever
 * @date 2018-04-11
 */
public class LogFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);
    private static final Map<ClassLoader, MdcCtxInfo> mdcCtxInfoCache = new ConcurrentHashMap<>(16);

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) {
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        Application application = (Application) filterContext.getAttribute("application");


        try {
            // 容器的IO线程MDC以及应用的MDC(不同classLoader)设置
            MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, transactionContext.sessionTid().orElse("0"));
            switchMdcToAppClassLoader("put", application.getAppClasssLoader(), transactionContext.sessionTid().orElse("0"));

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
                    + (soaHeader.getUserIp().isPresent() ? " userIp:" + soaHeader.getUserIp().get() : "");


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
                    switchMdcToAppClassLoader("remove", application.getAppClasssLoader(), transactionContext.sessionTid().orElse("0"));
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
                MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, transactionContext.sessionTid().orElse("0"));
                switchMdcToAppClassLoader("put", application.getAppClasssLoader(), transactionContext.sessionTid().orElse("0"));
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(getClass().getSimpleName()
                        + "::onExit:[seqId:" + transactionContext.seqId()
                        + ", execption:" + transactionContext.soaException()
                        + ",\n result:" + filterContext.getAttribute("result") + "]\n");
            }

            SoaHeader soaHeader = transactionContext.getHeader();

            Long requestTimestamp = (Long)transactionContext.getAttribute("dapeng_request_timestamp");

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
                switchMdcToAppClassLoader("remove", application.getAppClasssLoader(), transactionContext.sessionTid().orElse("0"));

                MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            }
        }
    }

    /**
     * 应用的MDC辅助类
     */
    class MdcCtxInfo {
        final ClassLoader appClassLoader;
        final Class<?> mdcClass;
        final Method put;
        final Method remove;

        MdcCtxInfo(ClassLoader app, Class<?> mdcClass, Method put, Method remove) {
            this.appClassLoader = app;
            this.mdcClass = mdcClass;
            this.put = put;
            this.remove = remove;
        }
    }

    // MDC.put(key, value), MDC.remove(key)
    private void switchMdcToAppClassLoader(String methodName, ClassLoader appClassLoader, String sessionTid) {
        try {
            MdcCtxInfo mdcCtxInfo = mdcCtxInfoCache.get(appClassLoader);
            if (mdcCtxInfo == null) {
                synchronized (appClassLoader) {
                    mdcCtxInfo = mdcCtxInfoCache.get(appClassLoader);
                    if (mdcCtxInfo == null) {
                        Class<?> mdcClass = appClassLoader.loadClass(MDC.class.getName());

                        mdcCtxInfo = new MdcCtxInfo(appClassLoader,
                                mdcClass,
                                mdcClass.getMethod("put", String.class, String.class),
                                mdcClass.getMethod("remove", String.class)
                        );
                        mdcCtxInfoCache.put(appClassLoader, mdcCtxInfo);
                    }
                }
            }


            if (methodName.equals("put")) {
                mdcCtxInfo.put.invoke(mdcCtxInfo.mdcClass, SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, sessionTid);
            } else {
                mdcCtxInfo.remove.invoke(mdcCtxInfo.mdcClass, SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            }
        } catch (ClassNotFoundException | NoSuchMethodException
                | IllegalAccessException |
                InvocationTargetException e) {
            LOGGER.error(getClass().getSimpleName() + "::switchMdcToAppClassLoader," + e.getMessage(), e);
        }
    }

}
