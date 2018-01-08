package com.github.dapeng.service.invoke.entity;

/**
 * Created by lihuimin on 2017/12/6.
 */
public class BaseRequest {

    private String jsonParameter;

    private String serviceName;

    private String versionName;

    private String methodName;

    public BaseRequest() {
    }

    public BaseRequest(String jsonParameter, String serviceName, String versionName, String methodName) {
        this.jsonParameter = jsonParameter;
        this.serviceName = serviceName;
        this.versionName = versionName;
        this.methodName = methodName;
    }

    public String getJsonParameter() {
        return jsonParameter;
    }

    public void setJsonParameter(String jsonParameter) {
        this.jsonParameter = jsonParameter;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
