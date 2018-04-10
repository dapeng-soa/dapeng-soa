package com.github.dapeng.eventbus.message.kafka;

import com.github.dapeng.eventbus.message.EventStore;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    private Boolean isAsync;

    public EventKafkaProducer(Boolean isAsync) {
        this.isAsync = isAsync;
//        init();
        createTransactionalProducer();
    }

    public void init() {
        KafkaConfigBuilder.ProducerConfiguration builder = KafkaConfigBuilder.defaultProducer();

        final Properties properties = builder.withKeySerializer(LongSerializer.class)
                .withValueSerializer(ByteArraySerializer.class)
                .bootstrapServers(kafkaConnect)
                .build();

        producer = new KafkaProducer<>(properties);
    }

    /**
     * 事务控制的 producer
     *
     * @return
     */
    protected void createTransactionalProducer() {
        KafkaConfigBuilder.ProducerConfiguration builder = KafkaConfigBuilder.defaultProducer();
        final Properties properties = builder.withKeySerializer(LongSerializer.class)
                .withValueSerializer(ByteArraySerializer.class)
                .bootstrapServers(kafkaConnect)
                .withTransactions("event")
                .build();
        producer = new KafkaProducer<>(properties);
        producer.initTransactions();
    }


    public void send(String topic, Long id, byte[] msg, Callback callback) {
        producer.send(new ProducerRecord<>(topic, id, msg), callback);
    }

    /**
     * batch to send message , if one is failed ,all batch message will  rollback.
     *
     * @param topic
     * @param eventMessage
     */
    public void batchSend(String topic, List<EventStore> eventMessage) {
        try {
            producer.beginTransaction();
            eventMessage.forEach(eventStore -> {
                producer.send(new ProducerRecord<>(topic, eventStore.getId(), eventStore.getEventBinary()), (metadata, exception) -> {
                    LOGGER.info("in transaction per msg ,send message to broker successful, " +
                                    "id: {}, topic: {}, offset: {}, partition: {}",
                            eventStore.getId(), metadata.topic(), metadata.offset(), metadata.partition());
                });
            });
            producer.commitTransaction();
        } catch (Exception e) {
            producer.abortTransaction();
            LOGGER.error(e.getMessage(), e);
            LOGGER.error("send message failed,topic: {}", topic);
            throw e;
        }
    }

}
