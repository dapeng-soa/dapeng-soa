package com.github.dapeng.message.consumer.container;

import com.github.dapeng.core.Application;
import com.github.dapeng.core.Plugin;
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
import java.util.List;

/**
 * Created by tangliu on 2016/9/18.
 */
public class KafkaMessagePlugin implements Plugin{

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessagePlugin.class);
    private MessageConsumerService consumerService = new com.github.dapeng.message.consumer.kafka.MessageConsumerServiceImpl();

    @SuppressWarnings("unchecked")
    @Override
    public void start() {

        try {
            //Collection<SoaServiceDefinition<?>> soaServiceDefinitions = processorMap.values();
            for (Application application : applications) {
                List<ServiceInfo> serviceInfos = application.getServiceInfos();

                for (ServiceInfo serviceInfo: serviceInfos) {
                    SoaServiceDefinition<?> definition = processorMap.get(new ProcessorKey(serviceInfo.serviceName,serviceInfo.version));
                    Class<?> ifaceClass = serviceInfo.ifaceClass;
                    Class MessageConsumerClass = null;
                    Class MessageConsumerActionClass = null;

                    try {
                        MessageConsumerClass = ifaceClass.getClassLoader().loadClass("com.github.dapeng.message.consumer.api.annotation.MessageConsumer");
                        MessageConsumerActionClass = ifaceClass.getClassLoader().loadClass("com.github.dapeng.message.consumer.api.annotation.MessageConsumerAction");
                    } catch (ClassNotFoundException e) {
                        LOGGER.info("无订阅服务或({})添加消息订阅失败:{}", ifaceClass.getName(), e.getMessage());
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
                                SoaFunctionDefinition functionDefinition = (SoaFunctionDefinition)definition.functions.get(methodName);

                                ConsumerContext consumerContext = new ConsumerContext();
                                consumerContext.setGroupId(groupId);
                                consumerContext.setTopic(topic);
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
