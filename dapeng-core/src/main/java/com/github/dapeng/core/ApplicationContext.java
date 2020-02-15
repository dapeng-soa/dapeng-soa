package com.github.dapeng.core;

import com.github.dapeng.core.definition.SoaServiceDefinition;

import java.util.List;
import java.util.Map;

public interface ApplicationContext {
    void start();

    Map<String,SoaServiceDefinition> getServiceDefinitions();

    void stop();
}
