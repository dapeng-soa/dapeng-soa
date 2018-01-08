package com.github.dapeng.core;

import java.util.List;
import java.util.Optional;

public interface Application {

    void start();

    void stop();

    List<ServiceInfo> getServiceInfos();

    void addServiceInfos(List<ServiceInfo> serviceInfos);

    void addServiceInfo(ServiceInfo serviceInfo);

    Optional<ServiceInfo> getServiceInfo(String name, String version);

    void info(Class<?> logClass, String formattedMsg, Object... args);

    void error(Class<?> logClass,String errMsg, Throwable exception);

    ClassLoader getAppClasssLoader();
}
