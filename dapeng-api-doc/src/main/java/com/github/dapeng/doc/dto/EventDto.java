package com.github.dapeng.doc.dto;

/**
 * @author with struy.
 * Create by 2018/2/25 01:59
 * email :yq1724555319@gmail.com
 */

public class EventDto {
    
    /**
     * 触发方法名
     */
    private String touchMethod;

    /**
     * 事件结构体名称
     */
    private String event;


    /**
     * 事件简称
     */
    private String shortName;

    /**
     * 事件简介
     */
    private String mark;

    public EventDto(String touchMethod, String event, String shortName, String mark) {
        this.touchMethod = touchMethod;
        this.event = event;
        this.shortName = shortName;
        this.mark = mark;
    }

    public String getTouchMethod() {
        return touchMethod;
    }

    public void setTouchMethod(String touchMethod) {
        this.touchMethod = touchMethod;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
