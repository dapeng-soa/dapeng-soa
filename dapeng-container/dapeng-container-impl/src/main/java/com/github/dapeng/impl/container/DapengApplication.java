package com.github.dapeng.impl.container;


import com.github.dapeng.core.Application;
import com.github.dapeng.core.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DapengApplication implements Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(DapengApplication.class);

    private static final Map<String, Object> loggerMap = new ConcurrentHashMap<>();

    private static final Map<String, Method> logMethodMap = new ConcurrentHashMap<>();

    private List<ServiceInfo> serviceInfos;

    private ClassLoader appClassLoader;

    public  DapengApplication(List<ServiceInfo> serviceInfos,ClassLoader appClassLoader ) {
        this.serviceInfos=Collections.unmodifiableList(serviceInfos);
        this.appClassLoader=appClassLoader;
    }

    @Override
    public List<ServiceInfo> getServiceInfos() {
        return this.serviceInfos;
    }

    @Override
    public void addServiceInfos(List<ServiceInfo> serviceInfos) {
        this.serviceInfos.addAll(serviceInfos);
    }

    @Override
    public void addServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfos.add(serviceInfo);
    }

    @Override
    public Optional<ServiceInfo> getServiceInfo(String name, String version) {
        return serviceInfos.stream().filter(i -> name.equals(i.serviceName) && version.equals(i.version)).findFirst();
    }

    @Override
    public void info(Class<?> logClass, String formattedMsg, Object... args) {

        methodInvoke(logClass,"info",logger -> logger.info(formattedMsg, args), new Object[]{formattedMsg, args});
    }

    @Override
    public void error(Class<?> logClass,String errMsg, Throwable exception) {

        methodInvoke(logClass,"error", logger -> logger.error(errMsg, exception), new Object[]{errMsg, exception});

    }

    @Override
    public ClassLoader getAppClasssLoader() {
        return this.appClassLoader;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private void methodInvoke(Class<?> logClass, String methodName, Consumer<Logger> loggerConsumer, Object... args) {
        try {

            if (this.appClassLoader != null) {
                Object appLogger = getLogger(appClassLoader, logClass, appClassLoader.hashCode());
                Method infoMethod = getMethod(methodName, logClass, appLogger, appClassLoader.hashCode());
                infoMethod.invoke(appLogger, args);
            } else {
                Logger containerLogger = LoggerFactory.getLogger(logClass);
                loggerConsumer.accept(containerLogger);
            }

        } catch (Exception e) {
            //有异常用容器的logger打日志
            LOGGER.error(e.getMessage());
            Logger containerLogger = LoggerFactory.getLogger(logClass);
            loggerConsumer.accept(containerLogger);
        }
    }

    public static Object getLogger(ClassLoader appClassLoader, Class<?> logClass, int classLoaderHex) throws Exception {
        Object logger;
        String logMethodKey= classLoaderHex+"."+logClass.getName();
        if (loggerMap.containsKey(logMethodKey)) {
            logger = loggerMap.get(logMethodKey);
        } else {
            Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
            Method getILoggerFactory = logFactoryClass.getMethod("getLogger", Class.class);
            getILoggerFactory.setAccessible(true);
            logger = getILoggerFactory.invoke(null, logClass);
            loggerMap.put(logMethodKey, logger);
        }
        return logger;
    }

    public static Method getMethod(String methodName, Class<?> logClass, Object logger, int classLoaderHex) throws Exception {
        Method method;
        String logMethodKey = classLoaderHex + "." + logClass.getName() + methodName;
        if (logMethodMap.containsKey(logMethodKey)) {
            method = logMethodMap.get(logMethodKey);
        } else {
            if (methodName.equals("error")) {
                method = logger.getClass().getMethod(methodName, String.class, Throwable.class);
            } else {
                method = logger.getClass().getMethod(methodName, String.class, Object[].class);
            }

            logMethodMap.put(logMethodKey, method);
        }
        return method;
    }

}
