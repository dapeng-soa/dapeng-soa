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
