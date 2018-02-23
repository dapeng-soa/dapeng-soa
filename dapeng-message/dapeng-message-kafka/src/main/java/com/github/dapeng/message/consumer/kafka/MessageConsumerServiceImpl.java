package com.github.dapeng.message.consumer.kafka;

import com.github.dapeng.message.consumer.api.context.ConsumerContext;
import com.github.dapeng.message.consumer.api.service.MessageConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tangliu on 2016/9/12.
 */
public class MessageConsumerServiceImpl implements MessageConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerServiceImpl.class);

    public static final Map<String, EventKafkaConsumer> TOPIC_CONSUMERS = new HashMap<>();

    @Override
    public void addConsumer(ConsumerContext context) {

        String groupId = context.getGroupId();
        String topic = context.getTopic();

        Class<?> ifaceClass = context.getIface().getClass();

        try {
            String className = context.getIface() instanceof Proxy ? ((Class) ifaceClass.getMethod("getTargetClass").invoke(context.getIface())).getName() : ifaceClass.getName();
            groupId = "".equals(groupId) ? className : ifaceClass.getName();
            String consumerKey = groupId + ":" + topic;

            if (TOPIC_CONSUMERS.containsKey(consumerKey)) {
                TOPIC_CONSUMERS.get(consumerKey).addCustomer(context);
            } else {
               // KafkaConsumer consumer = new KafkaConsumer(groupId, topic);
                EventKafkaConsumer consumer = new EventKafkaConsumer(groupId,topic);
                consumer.start();
                consumer.addCustomer(context);
                TOPIC_CONSUMERS.put(consumerKey, consumer);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }

    }

    @Override
    public void removeConsumer(ConsumerContext context) {
        String groupId = context.getGroupId();
        String topic = context.getTopic();
        String consumerKey = groupId + ":" + topic;

        TOPIC_CONSUMERS.remove(consumerKey);
    }

    @Override
    public void clearConsumers() {
        TOPIC_CONSUMERS.clear();
    }
}
