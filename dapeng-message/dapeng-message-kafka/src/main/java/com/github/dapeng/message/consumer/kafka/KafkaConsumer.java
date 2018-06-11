package com.github.dapeng.message.consumer.kafka;

import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.message.consumer.api.context.ConsumerContext;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.ByteBufferDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by tangliu on 2016/8/3.
 */
public class KafkaConsumer extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private List<ConsumerContext> customers = new ArrayList<>();

    private String groupId;

    private List<String>topics;

    public KafkaConsumer(String groupId) {
        this.groupId = groupId;
        init();
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    private String kafkaConnect = SoaSystemEnvProperties.SOA_KAFKA_PORT;

    protected org.apache.kafka.clients.consumer.KafkaConsumer<ByteBuffer, ByteBuffer> consumer;

    public void init() {

        logger.info(new StringBuffer("[KafkaConsumer] [init] ")
                .append("kafkaConnect(").append(kafkaConnect)
                .append(") groupId(").append(groupId)
                .append(")").append(")").toString());

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConnect);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", ByteBufferDeserializer.class);
        props.put("value.deserializer", ByteBufferDeserializer.class);

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);

    }

    @Override
    public void run() {

        try {
            logger.info("[KafkaConsumer][{}][run] ", groupId );

            consumer.subscribe(topics);
            while (true) {
                ConsumerRecords<ByteBuffer, ByteBuffer> records = consumer.poll(100);
                for (ConsumerRecord<ByteBuffer, ByteBuffer> record : records) {
                    String topic = record.topic();
                    receive(record.value(),topic);
                }
            }
        } catch (Exception e) {
            logger.error("[KafkaConsumer][{}][run] " + e.getMessage(), groupId , e);
        }
    }


    /**
     * Kafka Consumer接收到消息，调用方法消费消息
     *
     * @param message
     */
    private void receive(ByteBuffer message,String topic) {

        logger.info("KafkaConsumer groupId({}) 收到消息", groupId);
        for (ConsumerContext customer : customers) {
            if (customer.getTopic().equals(topic)) {
                dealMessage(customer, message);
            }
        }
    }

    /**
     * 添加一个订阅同一个topic的“客户端”,客户端可以理解为一个订阅消息的方法
     *
     * @param client
     */
    public void addCustomer(ConsumerContext client) {
        this.customers.add(client);
    }

    public List<ConsumerContext> getCustomers() {
        return customers;
    }

    public void setCustomers(List<ConsumerContext> customers) {
        this.customers = customers;
    }


    private void dealMessage(ConsumerContext customer, ByteBuffer message ) {

        SoaFunctionDefinition.Sync functionDefinition = (SoaFunctionDefinition.Sync)customer.getSoaFunctionDefinition();
        Object iface = customer.getIface();

        long count = new ArrayList<>(Arrays.asList(iface.getClass().getInterfaces()))
                .stream()
                .filter(m -> "org.springframework.aop.framework.Advised".equals(m.getName()))
                .count();

        Class<?> ifaceClass;
        try {
            ifaceClass = (Class) (count > 0 ? iface.getClass().getMethod("getTargetClass").invoke(iface) : iface.getClass());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            ifaceClass = iface.getClass();
        }

        Method [] declaredMethods = ((SoaFunctionDefinition.Sync) functionDefinition).getClass().getDeclaredMethods();
        Method method = declaredMethods[0];

        Object argsParam = null;

        if (!method.getReturnType().getName().contains("java.lang.Object")) {

            Parameter[] parameters = method.getParameters();
            for (Parameter param : parameters) {
                if (param.getType().getName().contains("args")) {
                    try {
                        argsParam = param.getType().newInstance();
                    } catch (Exception e) {
                        logger.error(" failed to instance method: {}" + method.getName());
                    }
                }
            }

            Field field = argsParam.getClass().getDeclaredFields()[0];
            field.setAccessible(true);//暴力访问，取消私有权限,让对象可以访问

            try {
                field.set(argsParam, message);
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(),e);
            }

        }else{
            method = declaredMethods[1];
            Parameter[] parameters = method.getParameters();
            Class[] inArgs = new Class[]{ByteBuffer.class};
            Object[] inArgsParms = new Object[]{message};
            for (Parameter param : parameters) {
                if (param.getType().getName().contains("args")) {
                    try {
                        Constructor constructor = param.getType().getDeclaredConstructor(inArgs);
                        constructor.setAccessible(true);
                        argsParam = constructor.newInstance(inArgsParms);
                    } catch (Exception e) {
                        logger.error(" failed to instance method: {}" + method.getName());
                    }
                }
            }

        }

        try {
            logger.info("{}收到kafka消息，执行{}方法", ifaceClass.getName(), functionDefinition.methodName);
            functionDefinition.apply(iface, argsParam);
            logger.info("{}收到kafka消息，执行{}方法完成", ifaceClass.getName(), functionDefinition.methodName);
        } catch (Exception e) {
            logger.error("{}收到kafka消息，执行{}方法异常", ifaceClass.getName(), functionDefinition.methodName);
            logger.error(e.getMessage(), e);
        }
    }

}
