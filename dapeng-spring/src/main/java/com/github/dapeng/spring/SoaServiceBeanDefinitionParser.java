package com.github.dapeng.spring;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * @author craneding
 * @date 16/1/19
 */
public class SoaServiceBeanDefinitionParser implements BeanDefinitionParser {

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        String ref = element.getAttribute("ref");

        if (StringUtils.isBlank(id)) {
            id = ref + "_SoaProcessor";
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SoaProcessorFactory.class);
        builder.addConstructorArgReference(ref);
        builder.addConstructorArgValue(ref);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);

        return beanDefinition;


    }

}
