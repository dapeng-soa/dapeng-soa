package com.github.dapeng.message.event;

import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

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
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaConnect);
        /**
         * 指定了“all”将会阻塞消息，这种设置性能最低，但是是最可靠的
         */
        props.put("acks", "all");
        props.put("retries", 1);
        //缓存每个分区未发送消息,缓冲区大小
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(props);
    }

    public void send(String topic, Long id, byte[] msg,Callback callback) {
        Future<RecordMetadata> send = producer.send(new ProducerRecord<>(topic, id, msg),callback);
    }

}
