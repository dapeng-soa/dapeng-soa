package com.github.dapeng.core;

import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.lifecycle.LifeCycleAware;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;

import java.util.Map;

/**
 * provide a DapengApp(service set)'s context for an service-container
 *
 * Dapeng provide a spring load which defines services via META-INF/spring/service.xml
 *
 * If you need a customize loader, such as SpringBoot, or others,
 * provide an {@see ApplicationConext} implementation and defines as service extension at
 * META-INF/service/com.github.dapeng.core.ApplicationContext file
 */
public interface ApplicationContext extends LifeCycleAware {

    Map<String,SoaServiceDefinition> getServiceDefinitions();

    default void onStart(LifeCycleEvent event) {

    }

    default void onStop(LifeCycleEvent event) {

    }


}
