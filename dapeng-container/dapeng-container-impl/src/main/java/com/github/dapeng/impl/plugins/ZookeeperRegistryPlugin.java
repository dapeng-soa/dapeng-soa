package com.github.dapeng.impl.plugins;


import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.impl.container.DapengApplication;
import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.registry.RegistryAgentProxy;
import com.github.dapeng.registry.zookeeper.RegistryAgentImpl;

import java.util.List;

public class ZookeeperRegistryPlugin implements AppListener, Plugin {

    final Container container;
    private RegistryAgent registryAgent;

    public ZookeeperRegistryPlugin(Container container) {
        this.container = container;
        container.registerAppListener(this);
    }

    @Override
    public void appRegistered(AppEvent event) {

        if (registryAgent != null) {
            DapengApplication application = (DapengApplication) event.getSource();
            //TODO: zookeeper注册是否允许部分失败？ 对于整个应用来说应该要保证完整性吧
            application.getServiceInfos().forEach(serviceInfo ->
                    registerService(serviceInfo.serviceName,serviceInfo.version)
            );
        }

        // Monitor ZK's config properties for service
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        DapengApplication application = (DapengApplication) event.getSource();
        application.getServiceInfos().forEach(serviceInfo ->
            unRegisterService(serviceInfo.serviceName,serviceInfo.version)
        );
    }

    @Override
    public void start() {

        registryAgent = new RegistryAgentImpl(false);

        RegistryAgentProxy.setCurrentInstance(RegistryAgentProxy.Type.Server, registryAgent);

        registryAgent.setProcessorMap(ContainerFactory.getContainer().getServiceProcessors());
        registryAgent.start();

        container.getApplications().forEach(app -> {
            List<ServiceInfo> serviceInfos = app.getServiceInfos();
            serviceInfos.forEach(s -> registerService(s.serviceName,s.version));
        });
    }

    @Override
    public void stop() {
        container.getApplications().forEach(app -> {
            List<ServiceInfo> serviceInfos = app.getServiceInfos();
            serviceInfos.forEach(s -> unRegisterService(s.serviceName,s.version));
        });
    }

    public void registerService(String serviceName, String version) {
        registryAgent.registerService(serviceName,version);
    }

    public void unRegisterService(String serviceName, String version) {
        System.out.println(" unRegister service: " + serviceName + " " + version);
    }
}
