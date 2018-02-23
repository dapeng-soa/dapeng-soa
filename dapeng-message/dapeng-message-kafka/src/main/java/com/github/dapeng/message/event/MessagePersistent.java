package com.github.dapeng.message.event;

import com.github.dapeng.message.event.serializer.KafkaMessageProcessor;
import com.github.dapeng.org.apache.thrift.TException;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月13日 下午3:07
 */
public abstract class MessagePersistent {

    public <T> void persistents(T event) throws TException {
        KafkaMessageProcessor<T> processor = new KafkaMessageProcessor<>();
        byte[] bytes = processor.buildMessageByte(event);
        saveMessageToDB(event.getClass().getName(),bytes);

    }

    public abstract void saveMessageToDB(String name, byte[] bytes);


}
