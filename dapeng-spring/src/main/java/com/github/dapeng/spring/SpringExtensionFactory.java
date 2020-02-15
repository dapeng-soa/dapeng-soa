package com.github.dapeng.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpringExtensionFactory {

    private static final Logger logger = LoggerFactory.getLogger(SpringExtensionFactory.class);

    private static final Map<String, ApplicationContext> contexts = new ConcurrentHashMap<>();

    private static ClassLoader appClassLoader;

    public static ClassLoader getAppClassLoader() {
        return appClassLoader;
    }

    public static void setAppClassLoader(ClassLoader appClassLoader) {
        SpringExtensionFactory.appClassLoader = appClassLoader;
    }

    public static void addApplicationContext(ApplicationContext context) {
        contexts.put("", context);
    }

    public static void clear() {
        contexts.clear();
    }

    public static Map<String, ApplicationContext> getContexts() {
        return contexts;
    }

}
