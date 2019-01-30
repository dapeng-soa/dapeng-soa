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
package com.github.dapeng.message.producer;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.message.config.KafkaConfigBuilder;
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
public class KafkaMsgProducer {

    private Logger LOGGER = LoggerFactory.getLogger(KafkaMsgProducer.class);
    /**
     * 127.0.0.1:9091,127.0.0.1:9092
     */
    private String kafkaConnect = SoaSystemEnvProperties.SOA_KAFKA_HOST;

    private Producer<Long, byte[]> producer;

    private final Boolean isAsync;

    public KafkaMsgProducer(Boolean isAsync) {
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


}
