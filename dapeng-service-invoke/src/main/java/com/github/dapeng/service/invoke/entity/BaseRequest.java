package com.github.dapeng.service.invoke.entity;

import java.util.Optional;

/**
 * Created by lihuimin on 2017/12/6.
 */
public class BaseRequest {

    private String jsonParameter;

    private String serviceName;

    private String versionName;

    private String methodName;

    private Optional customerName = Optional.empty();

    private Optional customerId = Optional.empty();

    private Optional operatorId = Optional.empty();

    private Optional operatorName = Optional.empty();

    private Optional calleeIp = Optional.empty();

    private Optional calleePort = Optional.empty();

    private Optional callerIp = Optional.empty();

    private Optional callerFrom = Optional.empty();

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


    public Optional getCustomerName() {
        return customerName;
    }

    public void setCustomerName(Optional customerName) {
        this.customerName = customerName;
    }

    public Optional getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Optional customerId) {
        this.customerId = customerId;
    }

    public Optional getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Optional operatorId) {
        this.operatorId = operatorId;
    }

    public Optional getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(Optional operatorName) {
        this.operatorName = operatorName;
    }

    public Optional getCalleeIp() {
        return calleeIp;
    }

    public void setCalleeIp(Optional calleeIp) {
        this.calleeIp = calleeIp;
    }

    public Optional getCalleePort() {
        return calleePort;
    }

    public void setCalleePort(Optional calleePort) {
        this.calleePort = calleePort;
    }

    public Optional getCallerIp() {
        return callerIp;
    }

    public void setCallerIp(Optional callerIp) {
        this.callerIp = callerIp;
    }

    public Optional getCallerFrom() {
        return callerFrom;
    }

    public void setCallerFrom(Optional callerFrom) {
        this.callerFrom = callerFrom;
    }
}