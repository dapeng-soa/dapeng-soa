package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.Plugin;

import java.util.ServiceLoader;

public class PluginLoader {

    public void startup() {
        ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class, PluginLoader.class.getClassLoader());

        for (Plugin plugin: plugins) {
            System.out.println(" what the????");
            plugin.start();
        }

    }
}
