package com.today.serializer;

/**
 * @author huyj
 * @Created 2019-02-21 16:25
 */
public class EventEntity {

    private Long id;
    private String eventType;
    private Byte[] eventBinary;

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

    public Byte[] getEventBinary() {
        return eventBinary;
    }

    public void setEventBinary(Byte[] eventBinary) {
        this.eventBinary = eventBinary;
    }
}
