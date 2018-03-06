package com.github.dapeng.core;

import com.github.dapeng.core.definition.SoaServiceDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Plugin {

    public static Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = new HashMap<>();

    public static List<Application> applications = new ArrayList<>();

    public void start();

    public void stop();
}
