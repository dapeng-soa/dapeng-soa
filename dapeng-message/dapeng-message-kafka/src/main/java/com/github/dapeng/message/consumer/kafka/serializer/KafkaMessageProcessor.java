package com.github.dapeng.message.consumer.kafka.serializer;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.message.consumer.kafka.MessageInfo;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述: kafka 消息 编解码器
 *
 * @author maple.lei
 * @date 2018年02月13日 上午11:39
 */
public class KafkaMessageProcessor<T> {

    private Logger LOGGER = LoggerFactory.getLogger(KafkaMessageProcessor.class);
    private BeanSerializer<T> beanSerializer;
    private byte[] realMessage;

    public KafkaMessageProcessor(BeanSerializer<T> beanSerializer) {
        this.beanSerializer = beanSerializer;
    }

    public KafkaMessageProcessor() {
    }

    public T dealMessage(byte[] message) {
        String serializerQualifyName = null;
        try {
            String eventType = getEventType(message);
            LOGGER.info("fetch eventType: {}", eventType);

            serializerQualifyName = assemblySerializerName(eventType);
            Class<?> serializerClazz = this.getClass().getClassLoader().loadClass(serializerQualifyName);
            beanSerializer = (BeanSerializer) serializerClazz.newInstance();
            MessageInfo<T> messageInfo = parseMessage(message);

            T event = messageInfo.getEvent();
            LOGGER.info("dealMessage:event {}", event.toString());
            return event;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            LOGGER.error("找不到对应的消息解码器 {}", serializerQualifyName);
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * decode kafka message
     *
     * @return
     */
    public MessageInfo<T> parseMessage(byte[] bytes) throws TException {
        TKafkaTransport kafkaTransport = new TKafkaTransport(bytes, TKafkaTransport.Type.Read);
        TCompactProtocol protocol = new TCompactProtocol(kafkaTransport);
        String eventType = kafkaTransport.getEventType();
        T event = beanSerializer.read(protocol);
        return new MessageInfo<T>(eventType, event);
    }

    /**
     * encoding kafka message
     *
     * @param event
     * @return
     * @throws TException
     */
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


    /**
     * 获取事件权限定名
     *
     * @param message
     * @return
     */
    public String getEventType(byte[] message) {
        int pos = 0;
        while (pos < message.length) {
            if (message[pos++] == (byte) 0) {
                break;
            }
        }
        byte[] subBytes = new byte[pos - 1];
        System.arraycopy(message, 0, subBytes, 0, pos - 1);

        realMessage = new byte[message.length - pos];
        System.arraycopy(message, pos, realMessage, 0, message.length - pos);
        return new String(subBytes);
    }


    /**
     * 拼接 serializer 全限定名
     *
     * @param eventType
     * @return
     */
    private String assemblySerializerName(String eventType) {
        try {
            String eventPackage = eventType.substring(0, eventType.lastIndexOf("."));
            String eventName = eventType.substring(eventType.lastIndexOf(".") + 1);
            String eventSerializerName = eventPackage + ".serializer." + eventName + "Serializer";
            return eventSerializerName;
        } catch (RuntimeException e) {
            LOGGER.error("组装权限定名出错");
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }


}
