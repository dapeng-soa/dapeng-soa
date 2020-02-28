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

import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.spring.annotation.DapengService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Dapeng Service Definition Registrar
 *
 * @see com.github.dapeng.spring.annotation.DapengService
 */
public class DapengComponentScanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.isBeanNameInUse(PostProcessor.class.getName())) {
            RootBeanDefinition bean = new RootBeanDefinition(PostProcessor.class);
            registry.registerBeanDefinition(PostProcessor.class.getName(), bean);
        }

    }

    static class PostProcessor implements BeanDefinitionRegistryPostProcessor {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            for (String name : registry.getBeanDefinitionNames()) {
                BeanDefinition definition = registry.getBeanDefinition(name);
                if (definition instanceof AnnotatedBeanDefinition) {
                    AnnotationMetadata metadata = ((AnnotatedBeanDefinition) definition).getMetadata();
                    if (metadata.hasAnnotation(DapengService.class.getName()) ||
                            metadata.hasMetaAnnotation(DapengService.class.getName())) {

                        Map<String, Object> annotationAtts = metadata.getAnnotationAttributes(DapengService.class.getName());

                        if (annotationAtts.containsKey("service")) {
                            Class<?> interfaceClass = (Class<?>) annotationAtts.get("service");
                            char[] realServiceNameAsChars = interfaceClass.getSimpleName().toCharArray();
                            realServiceNameAsChars[0] += 32;
                            String realServiceName = String.valueOf(realServiceNameAsChars);
                            ConstructorArgumentValues paras = new ConstructorArgumentValues();
                            paras.addIndexedArgumentValue(0, new RuntimeBeanReference(realServiceName));
                            paras.addIndexedArgumentValue(1, realServiceName);
                            paras.addIndexedArgumentValue(2, interfaceClass);

                            RootBeanDefinition serviceDef = new RootBeanDefinition(SoaProcessorFactory.class, paras, null);
                            serviceDef.setScope(BeanDefinition.SCOPE_SINGLETON);
                            serviceDef.setTargetType(SoaServiceDefinition.class);

                            registry.registerBeanDefinition(realServiceName + "-definition", serviceDef);
                        }
                    }
                }
            }
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        }
    }

}
