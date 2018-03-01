package com.github.dapeng.eventbus.message;

import java.sql.Timestamp;
import java.util.Arrays;

/**
 * 描述: 消息持久化对象
 *
 * @author hz.lei
 * @date 2018年02月23日 下午9:23
 */
public class EventStore {
    private Long id;
    private String eventType;
    private byte[] eventBinary;
    private Timestamp updateAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public byte[] getEventBinary() {
        return eventBinary;
    }

    public void setEventBinary(byte[] eventBinary) {
        this.eventBinary = eventBinary;
    }

    public Timestamp getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Timestamp updateAt) {
        this.updateAt = updateAt;
    }

    @Override
    public String toString() {
        return "EventInfo{" +
                "id=" + id +
                ", eventType='" + eventType + '\'' +
                ", eventBinary=" + Arrays.toString(eventBinary) +
                ", updateAt=" + updateAt +
                '}';
    }
}
