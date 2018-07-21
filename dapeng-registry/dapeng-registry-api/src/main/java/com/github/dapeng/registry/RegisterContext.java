package com.github.dapeng.registry;

/**
 * 描述:  服务注册上下文，包括服务名，版本，注册到zk 的 path
 *
 * @author hz.lei
 * @date 2018年03月20日 下午11:09
 */
public class RegisterContext {
    /**
     * 服务名
     */
    private String service;
    /**
     * 版本号
     */
    private String version;

    /**
     * like /soa/runtime/services/com.api.UserService
     */
    private String servicePath;
    /**
     * like 192.168.1.121:9081:1.0.0
     */
    private String instanceInfo;

    public RegisterContext(String service, String version, String servicePath, String instanceInfo) {
        this.service = service;
        this.version = version;
        this.servicePath = servicePath;
        this.instanceInfo = instanceInfo;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getInstanceInfo() {
        return instanceInfo;
    }

    public void setInstanceInfo(String instanceInfo) {
        this.instanceInfo = instanceInfo;
    }
}
