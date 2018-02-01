package com.github.dapeng.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author craneding
 * @date 16/1/19
 */
public class SoaServiceNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("service", new SoaServiceBeanDefinitionParser());
    }

}
