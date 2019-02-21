package com.github.dapeng.api;

public interface PluginFactorySpi {

    Plugin createPlugin(Container container);
}
