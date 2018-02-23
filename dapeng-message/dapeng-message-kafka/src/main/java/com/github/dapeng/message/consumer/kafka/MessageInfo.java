package com.github.dapeng.message.consumer.kafka;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月13日 上午11:52
 */
public class MessageInfo<T> {
    private String eventType;
    private T event;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T event) {
        this.event = event;
    }

    public MessageInfo(String eventType, T event) {
        this.eventType = eventType;
        this.event = event;
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
                "eventType='" + eventType + '\'' +
                ", event=" + event +
                '}';
    }
}
