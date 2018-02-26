package com.github.dapeng.message.event;

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
public class EventKafkaProducer {

    private Logger LOGGER = LoggerFactory.getLogger(EventKafkaProducer.class);
    /**
     * 127.0.0.1:9091,127.0.0.1:9092
     */
    private String kafkaConnect = SoaSystemEnvProperties.SOA_KAFKA_PORT;

    private Producer<Long, byte[]> producer;

    private final Boolean isAsync;

    public EventKafkaProducer(Boolean isAsync) {
        this.isAsync = isAsync;
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

    public void send(String topic, Long id, byte[] msg) {
        Future<RecordMetadata> send = producer.send(new ProducerRecord<>(topic, id, msg), (metadata, exception) -> {
            if (exception != null) {
                LOGGER.error(exception.getMessage(), exception);
                LOGGER.error("send message failed,topic: {}, id: {}", topic, id);
            }
            LOGGER.info("send message successful,topic: {}, id: {}", topic, id);
        });

        LOGGER.info("send message successful,topic: {}, id: {}", topic, id);
    }

    public void sendAsync(String topic, Long id, byte[] msg) {
        producer.send(new ProducerRecord<>(topic, id, msg),
                (metadata, exception) -> System.out.println("#offset: " + metadata.offset()));
    }
}
