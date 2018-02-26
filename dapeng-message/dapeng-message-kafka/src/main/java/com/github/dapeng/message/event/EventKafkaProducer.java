package com.github.dapeng.message.event;

import com.github.dapeng.message.config.KafkaConfigBuilder;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * 描述: 跨领域（跨系统）事件  kafka 生产者
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
        KafkaConfigBuilder.ProducerConfiguration builder = KafkaConfigBuilder.defaultProducer();

        final Properties properties = builder.withKeySerializer(LongSerializer.class)
                                             .withValueSerializer(ByteArraySerializer.class)
                                             .bootstrapServers(kafkaConnect)
                                             .build();

        producer = new KafkaProducer<>(properties);
    }

    public void send(String topic, Long id, byte[] msg, Callback callback) {
        producer.send(new ProducerRecord<>(topic, id, msg), callback);
    }
}
