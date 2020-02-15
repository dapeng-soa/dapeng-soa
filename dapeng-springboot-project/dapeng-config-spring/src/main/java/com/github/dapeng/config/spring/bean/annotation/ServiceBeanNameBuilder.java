package com.github.dapeng.config.spring.bean.annotation;

import com.github.dapeng.config.annotation.DapengService;
import com.github.dapeng.config.spring.util.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

class ServiceBeanNameBuilder {

    private static final String SEPARATOR = ":";

    private String interfaceClassName;

    private Environment environment;

    // Optional
    private String version;

    private ServiceBeanNameBuilder(String interfaceClassName, Environment environment) {
        this.interfaceClassName = interfaceClassName;
        this.environment = environment;
    }

    private ServiceBeanNameBuilder(Class<?> interfaceClass, Environment environment) {
        this(interfaceClass.getName(), environment);
    }

    private ServiceBeanNameBuilder(DapengService service, Class<?> interfaceClass, Environment environment) {
        this(AnnotationUtils.resolveInterfaceName(service, interfaceClass), environment);
        this.version(service.version());
    }

    public static ServiceBeanNameBuilder create(Class<?> interfaceClass, Environment environment) {
        return new ServiceBeanNameBuilder(interfaceClass, environment);
    }

    public static ServiceBeanNameBuilder create(DapengService service, Class<?> interfaceClass, Environment environment) {
        return new ServiceBeanNameBuilder(service, interfaceClass, environment);
    }


    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(value).append(SEPARATOR);
        }
    }

    public ServiceBeanNameBuilder version(String version) {
        this.version = version;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder("ServiceBean").append(SEPARATOR);
        // Required
        append(beanNameBuilder, interfaceClassName);
        // Optional
        append(beanNameBuilder, version);
        // Build and remove last ":"
        String rawBeanName = beanNameBuilder.substring(0, beanNameBuilder.length() - 1);
        // Resolve placeholders
        return environment.resolvePlaceholders(rawBeanName);
    }
}