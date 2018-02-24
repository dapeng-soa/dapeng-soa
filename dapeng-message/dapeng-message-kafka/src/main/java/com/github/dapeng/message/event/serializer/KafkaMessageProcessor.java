package com.github.dapeng.message.event.serializer;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.message.event.MessageInfo;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
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

    public T dealMessage(byte[] message, ClassLoader classLoader) throws TException {

        String eventType = getEventType(message);
        LOGGER.info("fetch eventType: {}", eventType);
        beanSerializer = assemblyBeanSerializer(eventType, classLoader);
        MessageInfo<T> messageInfo = parseMessage(message);

        T event = messageInfo.getEvent();
        LOGGER.info("dealMessage:event {}", event.toString());
        return event;
    }

    /**
     * decode kafka message
     *
     * @return
     */
    private MessageInfo<T> parseMessage(byte[] bytes) throws TException {
        TKafkaTransport kafkaTransport = new TKafkaTransport(bytes, TKafkaTransport.Type.Read);
        TCompactProtocol protocol = new TCompactProtocol(kafkaTransport);
        String eventType = kafkaTransport.getEventType();
        T event = beanSerializer.read(protocol);
        return new MessageInfo<>(eventType, event);
    }

    /**
     * encoding kafka message
     *
     * @param event
     * @return
     * @throws TException
     */
    public byte[] buildMessageByte(T event) throws TException {
        String eventType = event.getClass().getName();
        beanSerializer = assemblyBeanSerializer(eventType);

        byte[] bytes = new byte[8192];
        TKafkaTransport kafkaTransport = new TKafkaTransport(bytes, TKafkaTransport.Type.Write);
        TCompactProtocol protocol = new TCompactProtocol(kafkaTransport);
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
     * 根据event类名 构造event 编解码器对象
     *
     * @param eventType
     * @return
     */
    private BeanSerializer assemblyBeanSerializer(String eventType, ClassLoader classLoader) {
        String eventSerializerName = null;
        try {
            String eventPackage = eventType.substring(0, eventType.lastIndexOf("."));
            String eventName = eventType.substring(eventType.lastIndexOf(".") + 1);
            eventSerializerName = eventPackage + ".serializer." + eventName + "Serializer";

            Class<?> serializerClazz = classLoader.loadClass(eventSerializerName);
            BeanSerializer beanSerializer = (BeanSerializer) serializerClazz.newInstance();

            return beanSerializer;
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.error("组装权限定名出错!!");
            LOGGER.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            LOGGER.error("找不到对应的消息解码器 {}", eventSerializerName);
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private BeanSerializer assemblyBeanSerializer(String eventType) {
        String eventSerializerName = null;
        try {

            String eventPackage = eventType.substring(0, eventType.lastIndexOf("."));
            String eventName = eventType.substring(eventType.lastIndexOf(".") + 1);
            eventSerializerName = eventPackage + ".serializer." + eventName + "Serializer";

            Class<?> serializerClazz = this.getClass().getClassLoader().loadClass(eventSerializerName);
            BeanSerializer beanSerializer = (BeanSerializer) serializerClazz.newInstance();

            return beanSerializer;
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.error("组装权限定名出错!!");
            LOGGER.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            LOGGER.error("找不到对应的消息解码器 {}", eventSerializerName);
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
