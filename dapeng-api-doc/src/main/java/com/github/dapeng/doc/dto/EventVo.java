package com.github.dapeng.doc.dto;

import java.util.List;
import java.util.Objects;

/**
 * @author with struy.
 * Create by 2018/2/25 22:33
 * email :yq1724555319@gmail.com
 */

public class EventVo {

    /**
     * 触发方法列表
     */
    private List<String> touchMethods;

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

    public EventVo() {}

    public EventVo(List<String> touchMethods, String event, String shortName, String mark) {
        this.touchMethods = touchMethods;
        this.event = event;
        this.shortName = shortName;
        this.mark = mark;
    }

    public List<String> getTouchMethods() {
        return touchMethods;
    }

    public void setTouchMethods(List<String> touchMethods) {
        this.touchMethods = touchMethods;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    @Override
    public String toString() {
        return "EventVo{" +
                "touchMethods=" + touchMethods +
                ", event='" + event + '\'' +
                ", shortName='" + shortName + '\'' +
                ", mark='" + mark + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventVo eventVo = (EventVo) o;
        return Objects.equals(touchMethods, eventVo.touchMethods) &&
                Objects.equals(event, eventVo.event) &&
                Objects.equals(shortName, eventVo.shortName) &&
                Objects.equals(mark, eventVo.mark);
    }

    @Override
    public int hashCode() {

        return Objects.hash(touchMethods, event, shortName, mark);
    }
}
