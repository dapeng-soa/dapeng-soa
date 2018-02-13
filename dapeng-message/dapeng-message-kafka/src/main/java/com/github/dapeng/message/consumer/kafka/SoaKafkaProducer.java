package com.github.dapeng.message.consumer.kafka;

import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月12日 上午11:50
 */
public class SoaKafkaProducer extends Thread {
    private Logger LOGGER = LoggerFactory.getLogger(SoaKafkaProducer.class);
    /**
     * 127.0.0.1:9091,127.0.0.1:9092
     */
    private String kafkaConnect = SoaSystemEnvProperties.SOA_KAFKA_PORT;

    private Producer<Long, String> producer;
    private final Boolean isAsync;
    private final String topic;

    public SoaKafkaProducer(Boolean isAsync, String topic) {
        this.isAsync = isAsync;
        this.topic = topic;
        init();
    }

    public void init() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConnect);
        props.put("acks", "all");
        props.put("retries", 1);
        //缓存每个分区未发送消息
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(props);
    }

    @Override
    public void run() {
        LOGGER.info("start to producer message...");
    }

    public void send(String msg) {

        Future<RecordMetadata> send = producer.send(new ProducerRecord<Long, String>(topic, 1L, msg));

    }

    public void sendAsync(String msg) {
        producer.send(new ProducerRecord<Long, String>(topic, 1L, msg), new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                System.out.println("#offset: " + metadata.offset());
            }
        });
    }
}
