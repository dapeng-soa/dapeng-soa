package com.github.dapeng.message.consumer.kafka;

import com.github.dapeng.message.event.serializer.KafkaMessageProcessor;
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
    private void receive(byte[] message) throws TException {
        logger.info("KafkaConsumer groupId({}) topic({}) 收到消息", groupId, topic);
//        dealMessage(message);
    }

    /*private void dealMessage(byte[] message) throws TException {
        KafkaMessageProcessor processor = new KafkaMessageProcessor();
        Object event = processor.dealMessage(message);

        if (event != null) {
            System.out.println("dealMessage:===>" + event.toString());
        } else {
            System.out.println("解析出错");
        }
    }*/
}
