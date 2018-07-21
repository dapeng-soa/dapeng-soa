package com.github.dapeng.registry;

/**
 * Registry Agent Proxy
 * 只针对服务端注册中心使用
 *
 * @author craneding
 * @date 16/3/1
 */
public class RegistryAgentProxy {
    private static RegistryServerAgent registryAgentServer = null;

    public static RegistryServerAgent getCurrentInstance() {
        return RegistryAgentProxy.registryAgentServer;
    }

    public static void setCurrentInstance(RegistryServerAgent registryAgent) {
        RegistryAgentProxy.registryAgentServer = registryAgent;
    }
}
