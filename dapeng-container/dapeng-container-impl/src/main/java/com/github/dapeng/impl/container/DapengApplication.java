/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.impl.container;


import com.github.dapeng.core.Application;
import com.github.dapeng.core.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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

    private Object springContext;

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
        for (int i = 0; i < serviceInfos.size(); i++) {
            ServiceInfo serviceInfo = serviceInfos.get(i);
            if (name.equals(serviceInfo.serviceName) && version.equals(serviceInfo.version)) {
                return Optional.of(serviceInfo);
            }
        }
        return Optional.empty();
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
    public Object getSpringBean(String beanName) {
        System.out.println("位置：DapengApplication.getSpringBean ==> " + "[Thread.currentThread().getContextClassLoader() = " + Thread.currentThread().getContextClassLoader() + "]");
        Thread.currentThread().setContextClassLoader(appClassLoader);
        return ((ClassPathXmlApplicationContext)this.springContext).getBeanFactory().getBean(beanName);
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

    public void bindSpringContext(Object springContext) {
        this.springContext = springContext;
    }

}
