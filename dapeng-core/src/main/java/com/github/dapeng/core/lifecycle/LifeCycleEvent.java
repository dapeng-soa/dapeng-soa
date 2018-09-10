package com.github.dapeng.core.lifecycle;

/**
 * 四种状态：start、pause、masterChange、stop
 *
 * @author hui
 * @date 2018/7/27 0027 9:39
 */
public class LifeCycleEvent {
    public enum LifeCycleEventEnum {
        /**
         * dapeng 容器启动
         */
        START,
        PAUSE,
        MASTER_CHANGE,
        CONFIG_CHANGE,
        STOP
    }

    /**
     * 事件类型
     */
    private final LifeCycleEventEnum eventEnum;
    /**
     * service name
     */
    private String service;
    /**
     * 事件发生时附带的一些属性
     */
    private Object attachment;

    public LifeCycleEvent(final LifeCycleEventEnum eventEnum,
                          final String service,
                          final Object attachment) {
        this.eventEnum = eventEnum;
        this.service = service;
        this.attachment = attachment;
    }

    public LifeCycleEvent(LifeCycleEventEnum eventEnum, Object attachment) {
        this.eventEnum = eventEnum;
        this.attachment = attachment;
    }

    public LifeCycleEvent(LifeCycleEventEnum eventEnum) {
        this.eventEnum = eventEnum;
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
