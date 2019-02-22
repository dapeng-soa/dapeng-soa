package com.today.kafka;

import com.github.dapeng.scheduler.events.TaskEvent;
import com.today.serializer.KafkaMessageProcessor;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * 定时任务执行信息发送到kafka
 *
 * @author huyj
 * @Created 2019-02-18 14:54
 */
public class TaskMsgKafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(TaskMsgKafkaProducer.class);

    private Producer producer;
    static Properties props = new Properties();

    public TaskMsgKafkaProducer(String serverHost) {
        initProducerConfig(serverHost);
    }

    public TaskMsgKafkaProducer initProducerConfig(String serverHost) {
        props.put("bootstrap.servers", serverHost);
        //acks 指定了“all”将会阻塞消息，这种设置性能最低，但是是最可靠的
        props.put("acks", "all");
        // retries 如果配置为0，不会有重复发送消息的问题
        props.put("retries", 0);
        // batch.size 缓存每个分区未发送消息,缓冲区大小
        props.put("batch.size", 16384);
        // linger.ms 批量等待时间
        props.put("linger.ms", 1);
        // buffer.memory
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
//        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        //props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        return this;
    }


    public TaskMsgKafkaProducer withValueByteArraySerializer() {
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        return this;
    }

    public TaskMsgKafkaProducer withValueStringSerializer() {
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return this;
    }


    private TaskMsgKafkaProducer withTransactions(String transactionId) {
        /**
         * 若要开启事务支持，除了配置transId外，还要配置生产者开启幂等
         */
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionId);

        /**
         * 幂等性保证
         * 当设置为‘true’，生产者将确保每个消息正好一次复制写入到stream。
         * 如果‘false’，由于broker故障，生产者重试。即，可以在流中写入重试的消息。此设置默认是‘false’。
         * 请注意，启用幂等式需要将max.in.flight.requests.per.connection设置为1或者等于5(默认)，
         * ，重试次数不能为零。另外acks必须设置为“all”。如果这些值保持默认值，我们将覆盖默认值。
         * 如果这些值设置为与幂等生成器不兼容的值，则将抛出一个ConfigException异常。
         */
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);


        //开启事物 重试次数不能为零
        props.put("retries", 1);
        return this;
    }

    public void sendMsgByTransaction(String topic, Long id, String message) {
        try {
            producer.beginTransaction();
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<Long, String>(topic, id, message)).get();
            producer.commitTransaction();
            logger.info("in transaction per msg ,send message to broker successful,  id: {}, topic: {}, message:{}, offset: {}, partition: {}",
                    id, recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            ex.printStackTrace();
            // producer.abortTransaction();
            logger.error(ex.getMessage(), ex);
            logger.error("send message failed,topic: {},message:{}", topic, message);
//                throw ex;
        }
    }


    public void sendMsgByTransaction(String topic, Long id, byte[] message) {
        try {
            producer.beginTransaction();
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<Long, byte[]>(topic, id, message)).get();
            producer.commitTransaction();
            logger.info("in transaction per msg ,send message to broker successful,  id: {}, topic: {}, message:{}, offset: {}, partition: {}",
                    id, recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            ex.printStackTrace();
            // producer.abortTransaction();
            logger.error(ex.getMessage(), ex);
            logger.error("send message failed,topic: {},message:{}", topic, message);
//                throw ex;
        }
    }

    public void sendMsg(String topic, String message) {
        try {
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<String, String>(topic, message)).get();
            logger.info("send message to broker successful,topic: {},message:{}, offset: {}, partition: {}", recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("send message failed,topic: {}", topic);
//                throw ex;
        }
    }

    public void sendMsg(String topic, byte[] message) {
        try {
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<String, byte[]>(topic, message)).get();
            logger.info("send message to broker successful,topic: {},message:{}, offset: {}, partition: {}", recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            logger.error("send message failed,topic: {}", topic);
//                throw ex;
        }
    }


    public void sendTaskMessage(String topic, TaskEvent taskEvent) {
        KafkaMessageProcessor kafkaMessageProcessor = new KafkaMessageProcessor();
        byte[] msgs = new byte[0];
        try {
            msgs = kafkaMessageProcessor.encodeMessage(taskEvent);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        sendMsgByTransaction(topic, taskEvent.getId(), msgs);
    }


    public TaskMsgKafkaProducer createProducer() {
        producer = new KafkaProducer<String, String>(props);
        return this;
    }

    public TaskMsgKafkaProducer createProducerWithTran(String transactionId) {
        withTransactions(transactionId);
        producer = new KafkaProducer<String, String>(props);
        producer.initTransactions();
        return this;
    }

    public void closeProducer() {
        producer.close();
    }

}
