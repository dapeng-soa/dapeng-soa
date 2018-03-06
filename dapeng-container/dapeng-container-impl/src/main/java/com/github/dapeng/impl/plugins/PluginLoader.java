package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.Plugin;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class PluginLoader implements Plugin{


    private final List<ClassLoader> pluginClassLoaders;


    public PluginLoader(List<ClassLoader> pluginClassLoaders) {
        this.pluginClassLoaders = pluginClassLoaders;
    }


    @Override
    public void start() {

        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();
        List<Application> applications = ContainerFactory.getContainer().getApplications();

        for (Map.Entry<ProcessorKey, SoaServiceDefinition<?>> entry : processorMap.entrySet()) {
            Plugin.processorMap.put(entry.getKey(),entry.getValue());
        }
        for(Application application : applications){
            Plugin.applications.add(application);
        }

        for (ClassLoader pluginClassLoader: pluginClassLoaders) {
            ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class,pluginClassLoader);
            for (Plugin plugin: plugins) {
                System.out.println("start plugin:" + plugin.getClass().getName());
                plugin.start();
            }
        }

    }

    @Override
    public void stop() {

    }
}
