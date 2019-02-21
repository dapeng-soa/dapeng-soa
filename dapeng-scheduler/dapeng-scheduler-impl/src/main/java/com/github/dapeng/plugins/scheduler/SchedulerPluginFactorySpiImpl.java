package com.github.dapeng.plugins.scheduler;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.PluginFactorySpi;

public class SchedulerPluginFactorySpiImpl implements PluginFactorySpi {
    @Override
    public Plugin createPlugin(Container container) {
        return new TaskSchedulePlugin(container);
    }
}
