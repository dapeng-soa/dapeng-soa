package com.today.serializer;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * desc: kafka int 类型消息反序列化器. 如果消息 key 不为 long型,将返回 -1
 *
 * @author hz.lei
 * @since 2018年07月25日 下午6:47
 */
public class KafkaIntDeserializer implements Deserializer<Integer> {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // nothing to do
    }

    @Override
    public Integer deserialize(String topic, byte[] data) {
        if (data == null)
            return null;
        if (data.length != 4) {
            logger.error(" 收到的消息Key不是Integer类型,Size of data received length is not 4,key内容: " + new String(data));
            return -1;
        }

        int value = 0;
        for (byte b : data) {
            value <<= 8;
            value |= b & 0xFF;
        }
        return value;
    }

    @Override
    public void close() {
        // nothing to do
    }
}
