package com.github.dapeng.core.lifecycyle;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * @author hui
 * @date 2018/7/26 0026 9:36
 */

public class LifecycleProcessor {
    private static LifecycleProcessor instance = new LifecycleProcessor();

    private List<Lifecycle> lifecycles = new ArrayList<>(16);

    private LifecycleProcessor() {
    }

    public static LifecycleProcessor getInstance() {
        return instance;
    }

    /**
     * 对业务不同事件的响应
     */
    public void onLifecycleEvent(LifecycleEvent event) {
        switch (event) {
            case START:
                lifecycles.forEach(Lifecycle::onStart);
                break;
            case PAUSE:
                lifecycles.forEach(Lifecycle::onPause);
                break;
            case MASTER_CHANGE:
                lifecycles.forEach(Lifecycle::onMasterChange);
                break;
            case STOP:
                lifecycles.forEach(Lifecycle::onStop);
                break;
            default:
                throw new NotImplementedException();
        }
    }

    public void addLifecycles(final Collection<Lifecycle> lifecycles) {
        this.lifecycles.addAll(lifecycles);
    }
}

