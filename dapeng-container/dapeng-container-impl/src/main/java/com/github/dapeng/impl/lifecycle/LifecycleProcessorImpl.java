package com.github.dapeng.impl.lifecycle;

import com.github.dapeng.api.lifecycle.LifecycleProcessor;
import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author hui
 * @date 2018/7/26 0026 9:36
 */
public class LifecycleProcessorImpl implements LifecycleProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(LifecycleProcessorImpl.class);
    /**
     * key:service
     */
    private List<LifeCycleAware> lifeCycles = new ArrayList<>(16);

    protected LifecycleProcessorImpl() {
    }

    /**
     * 对业务不同事件的响应
     */
    @Override
    public void onLifecycleEvent(final LifeCycleEvent event) {
        switch (event.getEventEnum()) {
            case START:
                lifeCycles.forEach(lifeCycleAware -> {
                            try {
                                lifeCycleAware.onStart(event);
                            } catch (Throwable e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                );
                break;
            case PAUSE:
                lifeCycles.forEach(lifeCycleAware -> {
                            try {
                                lifeCycleAware.onPause(event);
                            } catch (Throwable e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                );
                break;
            case MASTER_CHANGE:
                lifeCycles.forEach(lifeCycleAware -> {
                            try {
                                lifeCycleAware.onMasterChange(event);
                            } catch (Throwable e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                );
                break;
            case CONFIG_CHANGE:
                lifeCycles.forEach(lifeCycleAware -> {
                            try {
                                lifeCycleAware.onConfigChange(event);
                            } catch (Throwable e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                );
                break;
            case STOP:
                lifeCycles.forEach(lifeCycleAware -> {
                            try {
                                lifeCycleAware.onStop(event);
                            } catch (Throwable e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                );
                break;
            default:
                throw new NotImplementedException();
        }
    }

    /**
     * 添加lifecyles
     *
     * @param lifecycles
     */
    @Override
    public void addLifecycles(final Collection<LifeCycleAware> lifecycles) {
        this.lifeCycles.addAll(lifecycles);
    }
}

