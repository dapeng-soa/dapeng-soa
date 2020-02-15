package com.github.dapeng.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 业务 ApplicationContext 和 application 等信息存储上下文
 */
public class SpringExtensionContext {
    private static final Logger logger = LoggerFactory.getLogger(SpringExtensionContext.class);

    private static ApplicationContext applicationContext;

    private static ClassLoader appClassLoader;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static ClassLoader getAppClassLoader() {
        return appClassLoader;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        if (SpringExtensionContext.applicationContext == null) {
            SpringExtensionContext.applicationContext = applicationContext;
            logger.info("Setting applicationContext {} to SpringExtensionContext", applicationContext);
        }
    }

    public static void setAppClassLoader(ClassLoader appClassLoader) {
        if (SpringExtensionContext.appClassLoader == null) {
            SpringExtensionContext.appClassLoader = appClassLoader;
            logger.info("Setting appClassLoader {} to SpringExtensionContext", appClassLoader);
        }
    }
}
