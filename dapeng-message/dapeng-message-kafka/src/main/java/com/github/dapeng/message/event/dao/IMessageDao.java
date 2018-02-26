package com.github.dapeng.message.event.dao;

import com.github.dapeng.message.event.task.EventStore;

import java.util.List;

/**
 * @author maple.lei
 */
public interface IMessageDao {

    /**
     * 查找所有的失败的或者未知的事务过程记录
     *
     * @param
     * @return
     */
    List<EventStore> listMessages();

    /**
     * fire事件后，将事件持久化到数据库中
     *
     * @param eventType
     * @param event
     * @return
     */
    int saveMessageToDB(String eventType, byte[] event);

    /**
     * kafka consumer消费消息后，删除持久化的消息
     *
     * @param eventId
     * @return
     */
    int deleteMessage(Long eventId);


}
