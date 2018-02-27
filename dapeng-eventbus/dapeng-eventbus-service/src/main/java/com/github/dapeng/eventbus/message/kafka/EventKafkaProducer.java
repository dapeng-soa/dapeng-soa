package com.github.dapeng.eventbus.message.kafka;

import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
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

    /**
     * 事务控制的 producer
     *
     * @return
     */
    protected Producer<Long, byte[]> createTransactionalProducer() {
        KafkaConfigBuilder.ProducerConfiguration builder = KafkaConfigBuilder.defaultProducer();
        final Properties properties = builder.withKeySerializer(LongSerializer.class)
                .withValueSerializer(ByteArraySerializer.class)
                .bootstrapServers(kafkaConnect)
                .withTransactions("event")
                .build();

        producer = new KafkaProducer<>(properties);
        producer.initTransactions();
        return producer;
    }


    public void send(String topic, Long id, byte[] msg, Callback callback) {
        producer.send(new ProducerRecord<>(topic, id, msg), callback);
    }

    /*public void batchSend(String topic, List<EventStore> msgs, TransCallback callback) {
        try {

            producer.beginTransaction();
            for (EventStore eventStore : msgs) {
                // todo
            }
            producer.commitTransaction();
            callback.onSuccess();
        } catch (Exception e) {
            producer.abortTransaction();
        }
    }*/

}
