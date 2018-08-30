package com.github.dapeng.api.lifecycle;

/**
 * @author ever
 */
public interface LifecycleProcessorFactorySpi {
    LifecycleProcessor createInstance();
}
