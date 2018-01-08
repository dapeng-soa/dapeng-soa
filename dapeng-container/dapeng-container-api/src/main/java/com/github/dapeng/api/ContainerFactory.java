package com.github.dapeng.api;


import java.util.List;
import java.util.ServiceLoader;

public class ContainerFactory {

    private static volatile Container applicationContainer;

    public static void createContainer(List<ClassLoader> applicationCls, ClassLoader containerCl) {
        if (applicationContainer == null) {
            synchronized (ContainerFactory.class) {
                ServiceLoader<ContainerFactorySpi> containerFactorySpis = ServiceLoader.load(ContainerFactorySpi.class, containerCl);
                assert containerFactorySpis.iterator().hasNext();
                applicationContainer = containerFactorySpis.iterator().next().createInstance(applicationCls);
            }
        }
    }

    public static Container getContainer() {
        return applicationContainer;
    }
}
