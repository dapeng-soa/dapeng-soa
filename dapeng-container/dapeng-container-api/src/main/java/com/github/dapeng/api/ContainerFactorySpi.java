package com.github.dapeng.api;

import java.util.List;

public interface ContainerFactorySpi {
    Container createInstance(List<ClassLoader> applicationCls, List<ClassLoader> pluginClassLoaders);
}
