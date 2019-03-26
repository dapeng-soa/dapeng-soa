/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.scheduler.kafka;

import com.github.dapeng.scheduler.api.enums.TaskStatusEnum;
import com.github.dapeng.scheduler.api.events.TaskEvent;
import com.github.dapeng.scheduler.kafka.serializer.KafkaMessageProcessor;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * 定时任务执行信息发送到kafka
 *
 * @author huyj
 * @Created 2019-02-18 14:54
 */
public class TaskMsgKafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(TaskMsgKafkaProducer.class);

    private Producer producer;
    private String topic;
    private String transIdPrefix;
    Properties props = new Properties();

    public TaskMsgKafkaProducer(String kafkaHost, String topic, boolean isTransactional, String transIdPrefix) {
        this.topic = topic;
        this.transIdPrefix = transIdPrefix;
        initProducerConfig(kafkaHost, true, isTransactional);
    }

    public TaskMsgKafkaProducer initProducerConfig(String kafkaHost, boolean isByteValueSerializered, boolean isTransactional) {
        props.put("bootstrap.servers", kafkaHost);
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

        if (isByteValueSerializered) {
            props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        } else {
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }

        if (isTransactional) {
            String transId = transIdPrefix + "-" + UUID.randomUUID().toString();
            createProducerWithTran(transId);
            logger.warn("kafka transaction producer is started successful with transID: " + transId);
        } else {
            createProducer();
        }
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
            producer.abortTransaction();
            logger.error(ex.getMessage(), ex);
            logger.error("send message failed,topic: {},message:{}", topic, message);
        }
    }


    private void sendMsgByTransaction(String topic, Long id, byte[] message) {
        try {
            producer.beginTransaction();
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<Long, byte[]>(topic, id, message)).get();
            producer.commitTransaction();
            logger.info("in transaction per msg ,send message to broker successful,  id: {}, topic: {}, message:{}, offset: {}, partition: {}",
                    id, recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            producer.abortTransaction();
            logger.error("send message failed,topic: {},message:{}", topic, message);
            logger.error(ex.getMessage(), ex);
        }
    }

    public void sendMsg(String topic, String message) {
        try {
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<String, String>(topic, message)).get();
            logger.info("send message to broker successful,topic: {},message:{}, offset: {}, partition: {}", recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            logger.error("send message failed,topic: {}", topic);
            logger.error(ex.getMessage(), ex);
        }
    }

    public void sendMsg(String topic, byte[] message) {
        try {
            RecordMetadata recordMetadata = (RecordMetadata) producer.send(new ProducerRecord<String, byte[]>(topic, message)).get();
            logger.info("send message to broker successful,topic: {},message:{}, offset: {}, partition: {}", recordMetadata.topic(), message, recordMetadata.offset(), recordMetadata.partition());
        } catch (Exception ex) {
            logger.error("send message failed,topic: {}", topic);
            logger.error(ex.getMessage(), ex);
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

    public void sendTaskMessageDefaultTopic(TaskEvent taskEvent) {
        KafkaMessageProcessor kafkaMessageProcessor = new KafkaMessageProcessor();
        byte[] msgs = new byte[0];
        try {
            msgs = kafkaMessageProcessor.encodeMessage(taskEvent);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        sendMsgByTransaction(topic, taskEvent.getId(), msgs);
    }

    public void sendTaskMessageDefaultTopic(Map map) {
        KafkaMessageProcessor kafkaMessageProcessor = new KafkaMessageProcessor();
        byte[] msgs = new byte[0];
        TaskEvent event = initTaskEvent(map);
        try {
            msgs = kafkaMessageProcessor.encodeMessage(event);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        sendMsgByTransaction(topic, event.getId(), msgs);
    }


    private TaskEvent initTaskEvent(Map eventMap) {
        TaskEvent taskEvent = new TaskEvent();
        taskEvent.setId(System.currentTimeMillis());
        taskEvent.setServiceName(eventMap.get("serviceName").toString());
        taskEvent.setMethodName(eventMap.get("methodName").toString());
        taskEvent.setVersion(eventMap.get("versionName").toString());
        taskEvent.setCostTime((long) eventMap.get("costTime"));
        taskEvent.setTaskStatus("success".equalsIgnoreCase(eventMap.get("taskStatus").toString()) ? TaskStatusEnum.SUCCEED : TaskStatusEnum.FAIL);
        taskEvent.setRemark(eventMap.get("remark").toString());
        return taskEvent;
    }

    public TaskMsgKafkaProducer createProducer() {
        producer = new KafkaProducer<String, String>(props);
        return this;
    }

    public TaskMsgKafkaProducer createProducerWithTran(String transactionId) {
        withTransactions(transactionId);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(KafkaProducer.class.getClassLoader());
        producer = new KafkaProducer<String, String>(props);
        producer.initTransactions();
        Thread.currentThread().setContextClassLoader(contextClassLoader);
        return this;
    }

    public void closeProducer() {
        producer.close();
    }
}
