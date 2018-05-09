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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DapengApplication implements Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(DapengApplication.class);

    // Map<LoggerName, Logger in appClassLoader>
    private final Map<String, Object> LOGER_MAP = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private Method slf4jInfoMethod, slf4jErrorMethod;

    private List<ServiceInfo> serviceInfos;

    private ClassLoader appClassLoader;

    public DapengApplication(List<ServiceInfo> serviceInfos, ClassLoader appClassLoader) {
        this.serviceInfos = Collections.unmodifiableList(serviceInfos);
        this.appClassLoader = appClassLoader;

        initSlf4jMethods();
    }

    @Override
    public List<ServiceInfo> getServiceInfos() {
        return this.serviceInfos;
    }

    @Override
    public void addServiceInfos(List<ServiceInfo> serviceInfos) {
        LOGGER.info(getClass().getSimpleName() + "::addServiceInfos serviceInfos[" + serviceInfos + "]");
        this.serviceInfos.addAll(serviceInfos);
    }

    @Override
    public void addServiceInfo(ServiceInfo serviceInfo) {
        LOGGER.info(getClass().getSimpleName() + "::addServiceInfo serviceInfo[" + serviceInfo + "]");
        this.serviceInfos.add(serviceInfo);
    }

    @Override
    public Optional<ServiceInfo> getServiceInfo(String name, String version) {
        return serviceInfos.stream().filter(i -> name.equals(i.serviceName) && version.equals(i.version)).findFirst();
    }

    @Override
    public void info(Class<?> logClass, String formattedMsg, Object... args) {

        methodInvoke(logClass, slf4jInfoMethod, logger -> logger.info(formattedMsg, args),
                new Object[]{formattedMsg, args});

    }

    @Override
    public void error(Class<?> logClass, String errMsg, Throwable exception) {

        methodInvoke(logClass, slf4jErrorMethod, logger -> logger.error(errMsg, exception),
                new Object[]{errMsg, exception});

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

    private void methodInvoke(Class<?> logClass, Method method, Consumer<Logger> loggerConsumer, Object... args) {
        try {

            if (this.appClassLoader != null) {
                Object appLogger = getLogger(appClassLoader, logClass);
                // Method infoMethod = getMethod(methodName, logClass, appLogger);
                method.invoke(appLogger, args);
            } else { // TODO
                Logger containerLogger = LoggerFactory.getLogger(logClass);
                loggerConsumer.accept(containerLogger);
            }

        } catch (Exception e) {
            //有异常用容器的logger打日志e
            LOGGER.error(e.getMessage(), e);
            Logger containerLogger = LoggerFactory.getLogger(logClass);
            loggerConsumer.accept(containerLogger);
        }
    }

    public Object getLogger(ClassLoader appClassLoader, Class<?> logClass) throws Exception {
        String logMethodKey = logClass.getName();
        if (LOGER_MAP.containsKey(logMethodKey)) {
            return LOGER_MAP.get(logMethodKey);
        }

        try {
            lock.lock();
            if (!LOGER_MAP.containsKey(logMethodKey)) {
                Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
                Method getILoggerFactory = logFactoryClass.getMethod("getLogger", Class.class);
                getILoggerFactory.setAccessible(true);
                Object logger = getILoggerFactory.invoke(null, logClass);
                LOGER_MAP.put(logMethodKey, logger);
                return logger;
            }
        } finally {
            lock.unlock();
        }

        return LOGER_MAP.get(logMethodKey);
    }

    private void initSlf4jMethods() {
        try {
            Class<?> loggerClass = appClassLoader.loadClass("org.slf4j.Logger");
            this.slf4jErrorMethod = loggerClass.getMethod("error", String.class, Throwable.class);
            this.slf4jInfoMethod = loggerClass.getMethod("info", String.class, Object[].class);
            LOGGER.info("init Slf4j OK");
        } catch (Exception ex) {
            LOGGER.error("init Slf4j Methods failed", ex);
        }
    }
}
