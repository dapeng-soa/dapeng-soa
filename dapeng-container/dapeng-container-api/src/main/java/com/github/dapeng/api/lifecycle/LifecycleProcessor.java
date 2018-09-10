package com.github.dapeng.api.lifecycle;

import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;

import java.util.Collection;

/**
 * @author ever
 */
public interface LifecycleProcessor {
    void onLifecycleEvent(final LifeCycleEvent event);
    void addLifecycles(final Collection<LifeCycleAware> lifecycles);
}
