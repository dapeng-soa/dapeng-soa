package com.github.dapeng.spring.boot.autoconfigure;

import com.github.dapeng.config.spring.bean.annotation.ServiceAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.*;

import java.util.Map;
import java.util.Set;

import static com.github.dapeng.spring.boot.autoconfigure.utils.AutoConfigurationUtils.*;
import static java.util.Collections.emptySet;

@Configuration
@ConditionalOnProperty(prefix = DAPENG_PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnClass(name = "com.github.dapeng.config.spring.bean.annotation.ServiceAnnotationBeanPostProcessor")
public class DapengSpringAutoConfiguration {


    @Bean(name = BASE_PACKAGES_PROPERTY_RESOLVER_BEAN_NAME)
    public PropertyResolver dubboScanBasePackagesPropertyResolver(ConfigurableEnvironment environment) {
        ConfigurableEnvironment propertyResolver = new AbstractEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                Map<String, Object> dubboScanProperties = getSubProperties(environment.getPropertySources(), DAPENG_SCAN_PREFIX);
                propertySources.addLast(new MapPropertySource("dapengScanProperties", dubboScanProperties));
            }
        };
        ConfigurationPropertySources.attach(propertyResolver);
        return new DelegatingPropertyResolver(propertyResolver);
    }

    @ConditionalOnProperty(prefix = DAPENG_SCAN_PREFIX, name = BASE_PACKAGES_PROPERTY_NAME)
    @ConditionalOnBean(name = BASE_PACKAGES_PROPERTY_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(ServiceAnnotationBeanPostProcessor.class)
    @Bean
    public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor(
            @Qualifier(BASE_PACKAGES_PROPERTY_RESOLVER_BEAN_NAME) PropertyResolver propertyResolver) {
        Set<String> packagesToScan = propertyResolver.getProperty(BASE_PACKAGES_PROPERTY_NAME, Set.class, emptySet());
        return new ServiceAnnotationBeanPostProcessor(packagesToScan);
    }

}
