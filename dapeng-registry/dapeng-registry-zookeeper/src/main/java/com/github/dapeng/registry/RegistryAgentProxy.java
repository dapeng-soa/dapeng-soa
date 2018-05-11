package com.github.dapeng.registry;

/**
 * Registry Agent Proxy
 * Fixme Server side only now
 * @author craneding
 * @date 16/3/1
 */
public class RegistryAgentProxy {
    private static RegistryAgent registryAgentServer = null;
    private static RegistryAgent registryAgentClient = null;

    public enum Type {
        Server, Client
    }

    /*
    public static synchronized void loadImpl(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        if (RegistryAgentProxy.registryAgent != null)
            throw new RuntimeException("com.github.dapeng.registry.registry agent is exist.");

        RegistryAgentProxy.registryAgent = (RegistryAgent) clazz.newInstance();
    }
    */

    public static RegistryAgent getCurrentInstance(Type type) {
        return type == Type.Server ? RegistryAgentProxy.registryAgentServer : RegistryAgentProxy.registryAgentClient;
    }

    public static void setCurrentInstance(Type type, RegistryAgent registryAgent) {
        if (type == Type.Server)
            RegistryAgentProxy.registryAgentServer = registryAgent;
        else
            RegistryAgentProxy.registryAgentClient = registryAgent;
    }
}
