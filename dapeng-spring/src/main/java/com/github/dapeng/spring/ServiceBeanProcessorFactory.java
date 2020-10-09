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

import com.github.dapeng.core.CustomConfig;
import com.github.dapeng.core.CustomConfigInfo;
import com.github.dapeng.core.Processor;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * ServiceBeanProcessorFactory for SpringBoot
 *
 * @author lei
 * @date 20/2/15
 */
public class ServiceBeanProcessorFactory implements ApplicationContextAware, FactoryBean<SoaServiceDefinition<?>> {

    private final Object serviceRef;

    public ServiceBeanProcessorFactory(Object serviceRef) {
        this.serviceRef = serviceRef;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SoaServiceDefinition<?> getObject() throws Exception {
        final Class<?> aClass = serviceRef.getClass();
        final List<Class<?>> interfaces = Arrays.asList(aClass.getInterfaces());

        List<Class<?>> filterInterfaces = interfaces.stream()
                .filter(anInterface -> anInterface.isAnnotationPresent(Service.class) && anInterface.isAnnotationPresent(Processor.class))
                .collect(toList());

        if (filterInterfaces.isEmpty()) {
            throw new RuntimeException("not config @Service & @Processor in " + serviceRef);
        }

        Class<?> interfaceClass = filterInterfaces.get(filterInterfaces.size() - 1);

        Processor processor = interfaceClass.getAnnotation(Processor.class);


        Class<?> processorClass = Class.forName(processor.className(), true, interfaceClass.getClassLoader());
        Constructor<?> constructor = processorClass.getConstructor(interfaceClass, Class.class);
        SoaServiceDefinition tProcessor = (SoaServiceDefinition) constructor.newInstance(serviceRef, interfaceClass);

        //idl service custom config
        if (interfaceClass.isAnnotationPresent(CustomConfig.class)) {
            CustomConfig customConfig = interfaceClass.getAnnotation(CustomConfig.class);
            long timeout = customConfig.timeout();
            tProcessor.setConfigInfo(new CustomConfigInfo(timeout));
        }
        // 过滤有 @CustomConfig 的方法
        Method[] serviceMethods = interfaceClass.getDeclaredMethods();
        List<Method> configMethod = Arrays.stream(serviceMethods)
                .filter(method -> tProcessor.functions.keySet().contains(method.getName()))
                .filter(method -> method.isAnnotationPresent(CustomConfig.class))
                .collect(Collectors.toList());

        //将值设置到 functions 中
        configMethod.forEach(method -> {
            CustomConfig customConfig = method.getAnnotation(CustomConfig.class);
            SoaFunctionDefinition functionDefinition = (SoaFunctionDefinition) tProcessor.functions.get(method.getName());
            functionDefinition.setCustomConfigInfo(new CustomConfigInfo(customConfig.timeout()));
        });

        return tProcessor;
    }

    @Override
    public Class<?> getObjectType() {
        return SoaServiceDefinition.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringExtensionContext.setApplicationContext(applicationContext);
    }
}
