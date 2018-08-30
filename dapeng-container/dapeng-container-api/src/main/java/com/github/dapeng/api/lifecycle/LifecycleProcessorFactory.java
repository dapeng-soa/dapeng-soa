package com.github.dapeng.api.lifecycle;


import java.util.ServiceLoader;

/**
 * @author ever
 */
public class LifecycleProcessorFactory {

    private static volatile LifecycleProcessor lifecycleProcessor;

    public static void createLifecycleProcessor(ClassLoader containerCl) {
        if (lifecycleProcessor == null) {
            synchronized (LifecycleProcessorFactory.class) {
                ServiceLoader<LifecycleProcessorFactorySpi> LifecycleProcessorFactorySpis = ServiceLoader.load(LifecycleProcessorFactorySpi.class, containerCl);
                assert LifecycleProcessorFactorySpis.iterator().hasNext();
                lifecycleProcessor = LifecycleProcessorFactorySpis.iterator().next().createInstance();
            }
        }
    }

    public static LifecycleProcessor getLifecycleProcessor() {
        return lifecycleProcessor;
    }
}
