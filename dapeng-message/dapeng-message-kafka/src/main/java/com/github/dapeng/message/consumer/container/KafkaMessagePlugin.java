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
package com.github.dapeng.message.consumer.container;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.message.consumer.api.context.ConsumerContext;
import com.github.dapeng.message.consumer.api.service.MessageConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by tangliu on 2016/9/18.
 */
public class KafkaMessagePlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessagePlugin.class);
    private MessageConsumerService consumerService = new com.github.dapeng.message.consumer.kafka.MessageConsumerServiceImpl();

    @SuppressWarnings("unchecked")
    @Override
    public void start() {

        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();
        List<Application> applications = ContainerFactory.getContainer().getApplications();
        try {
            //Collection<SoaServiceDefinition<?>> soaServiceDefinitions = processorMap.values();
            for (Application application : applications) {
                List<ServiceInfo> serviceInfos = application.getServiceInfos();

                for (ServiceInfo serviceInfo : serviceInfos) {
                    SoaServiceDefinition<?> definition = processorMap.get(new ProcessorKey(serviceInfo.serviceName, serviceInfo.version));
                    Class<?> ifaceClass = serviceInfo.ifaceClass;
                    Class MessageConsumerClass = null;
                    Class MessageConsumerActionClass = null;
                    try {
                        MessageConsumerClass = ifaceClass.getClassLoader().loadClass("com.github.dapeng.message.consumer.api.annotation.MessageConsumer");
                        MessageConsumerActionClass = ifaceClass.getClassLoader().loadClass("com.github.dapeng.message.consumer.api.annotation.MessageConsumerAction");
                    } catch (ClassNotFoundException e) {
                        LOGGER.info("({})添加消息订阅失败:{}", ifaceClass.getName(), e.getMessage());
                        break;
                    }

                    if (ifaceClass.isAnnotationPresent(MessageConsumerClass)) {

                        Annotation messageConsumer = ifaceClass.getAnnotation(MessageConsumerClass);
                        String groupId = (String) messageConsumer.getClass().getDeclaredMethod("groupId").invoke(messageConsumer);

                        for (Method method : ifaceClass.getMethods()) {
                            if (method.isAnnotationPresent(MessageConsumerActionClass)) {

                                String methodName = method.getName();

                                Annotation annotation = method.getAnnotation(MessageConsumerActionClass);
                                String topic = (String) annotation.getClass().getDeclaredMethod("topic").invoke(annotation);
                                //eventType
                                String eventType = (String) annotation.getClass().getDeclaredMethod("eventType").invoke(annotation);

                                SoaFunctionDefinition functionDefinition = (SoaFunctionDefinition) definition.functions.get(methodName);

                                ConsumerContext consumerContext = new ConsumerContext();
                                consumerContext.setGroupId(groupId);
                                consumerContext.setTopic(topic);
                                consumerContext.setEventType(eventType);
                                consumerContext.setIface(definition.iface);
                                consumerContext.setSoaFunctionDefinition(functionDefinition);

                                consumerService.addConsumer(consumerContext);

                                LOGGER.info("添加消息订阅({})({})", ifaceClass.getName(), method.getName());
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        consumerService.clearConsumers();
    }
}
