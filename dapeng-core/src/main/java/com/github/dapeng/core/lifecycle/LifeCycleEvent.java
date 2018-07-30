package com.github.dapeng.core.lifecycle;

/**
 * 四种状态：start、pause、masterChange、stop
 *
 * @author hui
 * @date 2018/7/27 0027 9:39
 */
public class LifeCycleEvent {
    public enum LifeCycleEventEnum {
        START,
        PAUSE,
        MASTER_CHANGE,
        STOP
    }

    /**
     * 事件发生时附带的一些属性
     */
    private final Object attachment;
    private final LifeCycleEventEnum eventEnum;
    /**
     * service name
     */
    private final String service;

    public LifeCycleEvent(final LifeCycleEventEnum eventEnum,
                          final String service,
                          final Object attachment) {
        this.attachment = attachment;
        this.eventEnum = eventEnum;
        this.service = service;
    }

    public Object getAttachment() {
        return attachment;
    }

    public LifeCycleEventEnum getEventEnum() {
        return eventEnum;
    }

    public String getService() {
        return service;
    }
}
