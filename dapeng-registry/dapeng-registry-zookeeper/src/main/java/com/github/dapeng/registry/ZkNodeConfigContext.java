package com.github.dapeng.registry;

/**
 * 描述: 基于zk 不同节点的配置信息bean
 *
 * @author hz.lei
 * @date 2018年03月16日 上午12:58
 */
public class ZkNodeConfigContext {

    private String globalConfig;

    private String serviceConfig;

    private String instanceConfig;

    public ZkNodeConfigContext(String globalConfig, String serviceConfig, String instanceConfig) {
        this.globalConfig = globalConfig;
        this.serviceConfig = serviceConfig;
        this.instanceConfig = instanceConfig;
    }

    public String getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(String globalConfig) {
        this.globalConfig = globalConfig;
    }

    public String getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(String serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public String getInstanceConfig() {
        return instanceConfig;
    }

    public void setInstanceConfig(String instanceConfig) {
        this.instanceConfig = instanceConfig;
    }
}
