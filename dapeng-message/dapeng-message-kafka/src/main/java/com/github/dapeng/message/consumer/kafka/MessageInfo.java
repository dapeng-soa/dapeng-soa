package com.github.dapeng.message.consumer.kafka;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月13日 上午11:52
 */
public class MessageInfo {
    private String eventType;
    private Object event;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }

    public MessageInfo(String eventType, Object event) {
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
