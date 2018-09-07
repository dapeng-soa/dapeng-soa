package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.filters.LogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Author: zhup
 * @Date: 2018/9/7 10:11
 * 应用MDC辅助类
 */

public class MdcCtxInfoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdcCtxInfoUtil.class);

    final ClassLoader appClassLoader;
    final Class<?> mdcClass;
    final Method put;
    final Method remove;

    MdcCtxInfoUtil(ClassLoader app, Class<?> mdcClass, Method put, Method remove) {
        this.appClassLoader = app;
        this.mdcClass = mdcClass;
        this.put = put;
        this.remove = remove;
    }

    // MDC.put(key, value), MDC.remove(key)
    public static void switchMdcToAppClassLoader(String methodName, ClassLoader appClassLoader, String sessionTid, Map<ClassLoader, MdcCtxInfoUtil> mdcCtxInfoCache) {
        try {
            MdcCtxInfoUtil mdcCtxInfo = mdcCtxInfoCache.get(appClassLoader);
            if (mdcCtxInfo == null) {
                synchronized (appClassLoader) {
                    mdcCtxInfo = mdcCtxInfoCache.get(appClassLoader);
                    if (mdcCtxInfo == null) {
                        Class<?> mdcClass = appClassLoader.loadClass(MDC.class.getName());

                        mdcCtxInfo = new MdcCtxInfoUtil(appClassLoader,
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
            LOGGER.error(appClassLoader.getClass().getSimpleName() + "::switchMdcToAppClassLoader," + e.getMessage(), e);
        }
    }
}
