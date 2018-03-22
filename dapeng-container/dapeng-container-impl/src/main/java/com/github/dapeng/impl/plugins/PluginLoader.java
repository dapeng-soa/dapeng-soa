package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.Plugin;

import java.util.ServiceLoader;

public class PluginLoader implements Plugin{

    private ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class, PluginLoader.class.getClassLoader());

    @Override
    public void start() {

        for (Plugin plugin: plugins) {
            plugin.start();
        }
    }

    @Override
    public void stop() {
        for (Plugin plugin: plugins) {

            plugin.stop();
        }
    }
}
