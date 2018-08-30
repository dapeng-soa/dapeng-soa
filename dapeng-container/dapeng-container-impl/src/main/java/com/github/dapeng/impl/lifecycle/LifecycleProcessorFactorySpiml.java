package com.github.dapeng.impl.lifecycle;

import com.github.dapeng.api.lifecycle.LifecycleProcessor;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactorySpi;
import com.github.dapeng.impl.plugins.monitor.DapengDoctor;

/**
 * @Author: ever
 * @Date: 2018/8/6 17:53
 */
public class LifecycleProcessorFactorySpiml implements LifecycleProcessorFactorySpi {
    @Override
    public LifecycleProcessor createInstance() {
        return new LifecycleProcessorImpl();
    }
}
