package com.github.dapeng.eventbus.message.service;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.timer.ScheduledTask;
import com.github.dapeng.core.timer.ScheduledTaskCron;
import com.github.dapeng.eventbus.api.message.service.MsgScheduledService;
import com.github.dapeng.eventbus.message.EventStore;
import com.github.dapeng.eventbus.message.dao.IMessageDao;
import com.github.dapeng.eventbus.message.kafka.EventKafkaProducer;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年02月27日 下午9:34
 */
@ScheduledTask
public class MessageScheduledService implements MsgScheduledService {

    private static Logger LOGGER = LoggerFactory.getLogger(MessageScheduledService.class);

    private String producerTopic = SoaSystemEnvProperties.SOA_EVENT_MESSAGE_TOPIC;

    @Autowired
    private IMessageDao messageDao;

    @Autowired
    private EventKafkaProducer producer;


    @Override
    @ScheduledTaskCron(cron = "*/5 * * * * ?")
    public void fetchMessage() throws SoaException {

        System.out.println("what are you doing ???");


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
                    LOGGER.info("send message to broker successful, id: {}, topic: {}, offset: {}, partition: {}",
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


   /* public void transFetchMessage() {
        List<EventStore> eventStores = messageDao.listMessages();
        if (!eventStores.isEmpty()) {
            producer.batchSend(producerTopic, eventStores, new TransCallback() {
                @Override
                public void onSuccess() {

                    //doDeleteMessage(eventStores);
                }
            });
        } else {
            LOGGER.debug("no event to send");
        }

    }*/
}
