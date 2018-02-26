package com.github.dapeng.message.event.task;

import com.github.dapeng.message.event.EventKafkaProducer;
import com.github.dapeng.message.event.dao.IMessageDao;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月23日 下午9:21
 */
@Transactional(rollbackFor = Exception.class)
public class MessageScheduled {
    private static Logger LOGGER = LoggerFactory.getLogger(MessageScheduled.class);

    private String producerTopic = SoaSystemEnvProperties.SOA_EVENT_MESSAGE_TOPIC;

    @Autowired
    private IMessageDao messageDao;

    @Autowired
    private EventKafkaProducer producer;


    public void fetchMessage() {
        List<EventStore> eventStores = messageDao.listMessages();
        if (!eventStores.isEmpty()) {
            eventStores.forEach(eventInfo -> {
                producer.send(producerTopic, eventInfo.getId(), eventInfo.getEventBinary(), (metadata, exception) -> {
                    if (exception != null) {
                        // 是否 抛异常
                        LOGGER.error(exception.getMessage(), exception);
                        LOGGER.error("send message failed,topic: {}, id: {}", producerTopic, eventInfo.getId());
                        //todo handleException
                    }
                    LOGGER.info("send message successful, id: {}, topic: {}, offset: {}, partition: {}",
                            eventInfo.getId(), metadata.topic(), metadata.offset(), metadata.partition());

                    doDeleteMessage(eventInfo);
                });
            });
        } else {
            LOGGER.debug("no event to send");
        }

    }

    private void doDeleteMessage(EventStore eventStore) {
        //fixme 便于测试。。。
        messageDao.deleteMessage(eventStore.getId());
        LOGGER.info("消息发送kafka broker 成功，删除message，id: {}", eventStore.getId());
    }


}
