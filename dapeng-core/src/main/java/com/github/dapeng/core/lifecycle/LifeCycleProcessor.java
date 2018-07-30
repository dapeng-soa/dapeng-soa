package com.github.dapeng.core.lifecycle;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * @author hui
 * @date 2018/7/26 0026 9:36
 */

public class LifeCycleProcessor {
    private static LifeCycleProcessor instance = new LifeCycleProcessor();

    /**
     * key:service
     */
    private List<LifeCycleAware> lifeCycles = new ArrayList<>(16);

    private LifeCycleProcessor() {
    }

    public static LifeCycleProcessor getInstance() {
        return instance;
    }

    /**
     * 对业务不同事件的响应
     */
    public void onLifecycleEvent(final LifeCycleEvent event) {
        switch (event.getEventEnum()) {
            case START:
                lifeCycles.forEach(lifeCycleAware -> {
                    lifeCycleAware.onStart(event);
                });
                break;
            case PAUSE:
                lifeCycles.forEach(lifeCycleAware -> {
                    lifeCycleAware.onPause(event);
                });
                break;
            case MASTER_CHANGE:
                lifeCycles.forEach(lifeCycleAware -> {
                    lifeCycleAware.onMasterChange(event);
                });
                break;
            case STOP:
                lifeCycles.forEach(lifeCycleAware -> {
                    lifeCycleAware.onStop(event);
                });
                break;
            default:
                throw new NotImplementedException();
        }
    }

    /**
     * 添加lifecyles
     * @param lifecycles
     */
    public void addLifecycles(final Collection<LifeCycleAware> lifecycles) {
        this.lifeCycles.addAll(lifecycles);
    }
}

