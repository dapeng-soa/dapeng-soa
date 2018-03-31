package com.github.dapeng.eventbus.message.dao;

import com.github.dapeng.eventbus.message.EventStore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author hz.lei
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
     * 单条删除
     *
     * @param eventId
     */
    void deleteMessage(Long eventId);

    /**
     * 批量删除
     *
     * @param eventStores
     */
    void deleteBatchMessage(List<EventStore> eventStores);

}
