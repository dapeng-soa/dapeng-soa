package com.github.dapeng.api.events;

import com.github.dapeng.core.Application;
import com.github.dapeng.core.Application;

import java.util.EventObject;

public class AppEvent extends EventObject {

    private AppEventType eventType;

    public AppEvent(Application application, AppEventType eventType) {
        super(application);
        this.eventType = eventType;
    }

    public AppEventType getEventType() {
        return this.eventType;
    }
}
