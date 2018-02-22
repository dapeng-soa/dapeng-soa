package com.github.dapeng.message.consumer.kafka;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.message.consumer.kafka.serializer.KafkaMessageProcessor;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月22日 上午11:50
 */
public class SoaKafkaConsumer extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(SoaKafkaConsumer.class);

    private String groupId;
    private String topic;
    private byte[] realMessage;

    public SoaKafkaConsumer(String groupId, String topic) {
        this.groupId = groupId;
        this.topic = topic;
        init();
    }

    private String kafkaConnect = SoaSystemEnvProperties.SOA_KAFKA_PORT;

    protected org.apache.kafka.clients.consumer.KafkaConsumer<Long, byte[]> consumer;

    public void init() {

        logger.info(new StringBuffer("[KafkaConsumer] [init] ")
                .append("kafkaConnect(").append(kafkaConnect)
                .append(") groupId(").append(groupId)
                .append(") topic(").append(topic).append(")").toString());

        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConnect);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", LongDeserializer.class);
        props.put("value.deserializer", ByteArrayDeserializer.class);
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }

    @Override
    public void run() {

        try {
            logger.info("[KafkaConsumer][{}][run] ", groupId + ":" + topic);

            consumer.subscribe(Arrays.asList(topic));
            while (true) {
                ConsumerRecords<Long, byte[]> records = consumer.poll(100);
                for (ConsumerRecord<Long, byte[]> record : records) {
                    receive(record.value());
                }
            }
        } catch (Exception e) {
            logger.error("[KafkaConsumer][{}][run] " + e.getMessage(), groupId + ":" + topic, e);
        }
    }


    /**
     * Kafka Consumer接收到消息，调用方法消费消息
     *
     * @param message
     */
    private void receive(byte[] message) throws TException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        logger.info("KafkaConsumer groupId({}) topic({}) 收到消息", groupId, topic);
        System.out.println("=====>  " + new String(message));
        dealMessage(message);
    }

    private void dealMessage(/*BeanSerializer serializer, */byte[] message) throws TException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String eventType = getEventType(message);
        System.out.println("====> eventType:" + eventType);

        String serializerQualifyName = assemblySerializerName(eventType);
        Class<?> serializerClazz = this.getClass().getClassLoader().loadClass(serializerQualifyName);
        BeanSerializer serializer = (BeanSerializer) serializerClazz.newInstance();

        KafkaMessageProcessor processor = new KafkaMessageProcessor(serializer);
        MessageInfo messageInfo = processor.parseMessage(message);
        Object event = messageInfo.getEvent();

        System.out.println("dealMessage:===>"+event.toString());

        /*try {
            logger.info("{}收到kafka消息，执行{}方法", ifaceClass.getName(), functionDefinition.methodName);
            functionDefinition.apply(iface, null);
            logger.info("{}收到kafka消息，执行{}方法完成", ifaceClass.getName(), functionDefinition.methodName);
        } catch (Exception e) {
            logger.error("{}收到kafka消息，执行{}方法异常", ifaceClass.getName(), functionDefinition.methodName);
            logger.error(e.getMessage(), e);
        }*/
    }

    /**
     * 拼接 serializer 全限定名
     *
     * @param eventType
     * @return
     */
    private String assemblySerializerName(String eventType) {
        String eventPackage = eventType.substring(0, eventType.lastIndexOf("."));
        String eventName = eventType.substring(eventType.lastIndexOf(".") + 1);
        String eventSerializerName = eventPackage + ".serializer." + eventName + "Serializer";

        return eventSerializerName;
    }

    public static void main(String[] args) {
        SoaKafkaConsumer consumer = new SoaKafkaConsumer("1", "2");
        consumer.getEventType("qwe0ddd".getBytes());
    }


    public String getEventType(byte[] message) {
        int pos = 0;
        while (pos < message.length) {
            if (message[pos++] == (byte) 0) {
                break;
            }
        }
        byte[] subBytes = new byte[pos - 1];
        System.arraycopy(message, 0, subBytes, 0, pos - 1);

        realMessage = new byte[message.length - pos];
        System.arraycopy(message, pos, realMessage, 0, message.length - pos);
        return new String(subBytes);
    }

}
