package com.github.dapeng.impl.container;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactorySpi;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactorySpi;

import java.util.List;

public class DapengContainerFactorySpiml implements ContainerFactorySpi {
    @Override
    public Container createInstance(List<ClassLoader> applicationCls) {
        return new DapengContainer(applicationCls);
    }
}
