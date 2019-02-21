/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.PluginFactorySpi;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * PluginLoader for 3rd pluginSpis, which are located at /dapeng-container/pluginSpis/
 * @author ever
 */
public class PluginLoader {
    private final List<ServiceLoader<PluginFactorySpi>> pluginSpis;

    private final List<Plugin> plugins = new ArrayList<>();

    public PluginLoader(List<ClassLoader> pluginCls) {
        this.pluginSpis = pluginCls.parallelStream()
                .map(pluginCl -> ServiceLoader.load(PluginFactorySpi.class, pluginCl))
                .collect(Collectors.toList());
    }

    public void startPlugins(Container container) {
        pluginSpis.forEach(spi -> spi.forEach(factory -> {
            Plugin plugin = factory.createPlugin(container);
            plugins.add(plugin);
            plugin.start();
        }));
    }

    public void stopPlugins() {
        plugins.forEach(Plugin::stop);
    }
}
