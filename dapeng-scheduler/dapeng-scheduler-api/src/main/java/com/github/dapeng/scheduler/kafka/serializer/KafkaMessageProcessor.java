package com.github.dapeng.scheduler.kafka.serializer;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.util.TKafkaTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述: kafka 消息 编解码器
 *
 * @author maple.lei
 * @since 2018年02月13日 上午11:39
 */
public class KafkaMessageProcessor<T> {

    private Logger logger = LoggerFactory.getLogger(KafkaMessageProcessor.class);

    private byte[] realMessage;

    /**
     * decode message
     *
     * @param
     * @param beanSerializer
     * @return
     * @throws TException
     */
    public T decodeMessage(byte[] msgBytes, BeanSerializer<T> beanSerializer) throws TException {
        TKafkaTransport kafkaTransport = new TKafkaTransport(msgBytes, TKafkaTransport.Type.Read);
        TCompactProtocol protocol = new TCompactProtocol(kafkaTransport);

        T event = beanSerializer.read(protocol);

        logger.debug("[decode] 解码消息 event: {}", event.toString());
        return event;
    }

    /**
     * encoding kafka message
     *
     * @param event
     * @return
     * @throws TException
     */
    public byte[] encodeMessage(T event) throws TException {
        String eventType = event.getClass().getName();
        BeanSerializer<T> beanSerializer = assemblyBeanSerializer(eventType);

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

    public byte[] getEventBinary() {
        return realMessage;
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
            logger.error("组装权限定名出错!!");
            logger.error(e.getMessage(), e);
        } catch (IllegalAccessException | InstantiationException e) {
            logger.error(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.error("找不到对应的消息解码器 {}", eventSerializerName);
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
