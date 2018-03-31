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
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年02月27日 下午9:34
 */
//@ScheduledTask
@Transactional(rollbackFor = Exception.class)
public class MsgScheduledServiceImpl implements MsgScheduledService {

    private static Logger LOGGER = LoggerFactory.getLogger(MsgScheduledServiceImpl.class);
    /**
     * producer topic
     */
    private String producerTopic = SoaSystemEnvProperties.SOA_EVENT_MESSAGE_TOPIC;

    /**
     * should be setter
     */
    private IMessageDao messageDao;

    public IMessageDao getMessageDao() {
        return messageDao;
    }

    public void setMessageDao(IMessageDao messageDao) {
        this.messageDao = messageDao;
    }

    private EventKafkaProducer producer = new EventKafkaProducer(false);

    @SuppressWarnings("AlibabaRemoveCommentedCode")
    @Override
//    @ScheduledTaskCron(cron = "*/5 * * * * ?")
    public void fetchMessage() throws SoaException {
        // fetch distribute lock
        //conn.createStatement().executeQuery("SELECT * FROM GLOBAL_LOCK WHERE BIZ_TAG = '" + producerTopic + "' FOR UPDATE ");

        try {
            List<EventStore> eventStores = messageDao.listMessages();
            if (!eventStores.isEmpty()) {
                producer.batchSend(producerTopic, eventStores);
                //可能消息发送成功，但是在删除消息时出错，事务回滚，消息未删除，导致下一次定时器消息发送重复
                messageDao.deleteBatchMessage(eventStores);
                LOGGER.info("消息发送kafka broker 成功，删除message");
            } else {
                LOGGER.debug("database no message to process ");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
