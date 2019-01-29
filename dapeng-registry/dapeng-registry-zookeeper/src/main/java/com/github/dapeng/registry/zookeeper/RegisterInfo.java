package com.github.dapeng.registry.zookeeper;

/**
 * 描述:  服务注册信息，包括服务名，版本，注册到zk 的 path
 *
 * @author hz.lei
 * @date 2018年03月20日 下午11:09
 */
public class RegisterInfo {
    /**
     * 服务名
     */
    private final String service;
    /**
     * 版本号
     */
    private final String version;

    /**
     * like /soa/runtime/services/com.api.UserService
     */
    private final String servicePath;
    /**
     * like 192.168.1.121:9081:1.0.0
     */
    private final String instanceInfo;

    public RegisterInfo(final String service, final String version, final String servicePath, final String instanceInfo) {
        this.service = service;
        this.version = version;
        this.servicePath = servicePath;
        this.instanceInfo = instanceInfo;
    }

    public String getService() {
        return service;
    }

    public String getVersion() {
        return version;
    }


    public String getServicePath() {
        return servicePath;
    }

    public String getInstanceInfo() {
        return instanceInfo;
    }
}
