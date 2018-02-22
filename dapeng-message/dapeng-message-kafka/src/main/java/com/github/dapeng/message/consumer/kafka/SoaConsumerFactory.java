package com.github.dapeng.message.consumer.kafka;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月22日 下午4:54
 */
public class SoaConsumerFactory {

    private String groupId;
    private String topic;

    public SoaConsumerFactory(String groupId, String topic) {
        this.groupId = groupId;
        this.topic = topic;
        startup();
    }

    public void startup() {
        SoaKafkaConsumer consumer = new SoaKafkaConsumer(groupId,topic);
        consumer.start();
    }



}
