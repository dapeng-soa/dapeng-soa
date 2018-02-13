package com.github.dapeng.message.consumer.kafka.serializer;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.message.consumer.kafka.MessageInfo;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
/**
 * 描述: kafka 消息 解析器
 *
 * @author maple.lei
 * @date 2018年02月13日 上午11:39
 */
public class KafkaMessageProcessor<T> {

    private BeanSerializer<T> beanSerializer;

    public KafkaMessageProcessor(BeanSerializer<T> beanSerializer) {
        this.beanSerializer = beanSerializer;
    }

    /**
     * decode kafka message
     *
     * @return
     */
    public MessageInfo parseMessage(byte[] bytes) throws TException {
        TKafkaTransport kafkaTransport = new TKafkaTransport(bytes, TKafkaTransport.Type.Read);
        TCompactProtocol protocol = new TCompactProtocol(kafkaTransport);
        String eventType = kafkaTransport.getEventType();
        T event = beanSerializer.read(protocol);
        return new MessageInfo(eventType, event);
    }

    public byte[] buildMessageByte(T event) throws TException {
        byte[] bytes = new byte[8192];
        TKafkaTransport kafkaTransport = new TKafkaTransport(bytes, TKafkaTransport.Type.Write);
        TCompactProtocol protocol = new TCompactProtocol(kafkaTransport);
        String eventType = event.getClass().getName();
        kafkaTransport.setEventType(eventType);
        beanSerializer.write(event, protocol);
        kafkaTransport.flush();
        bytes = kafkaTransport.getByteBuf();
        return bytes;
    }
}
