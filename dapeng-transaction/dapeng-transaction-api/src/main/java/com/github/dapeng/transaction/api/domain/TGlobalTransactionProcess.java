package com.github.dapeng.transaction.api.domain;

import java.io.Serializable;

/**
 *
 **/
public class TGlobalTransactionProcess implements Serializable{

    /**
     *
     **/
    private Integer id;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    /**
     *
     **/
    private Integer transactionId;

    public Integer getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }


    /**
     *
     **/
    private Integer transactionSequence;

    public Integer getTransactionSequence() {
        return this.transactionSequence;
    }

    public void setTransactionSequence(Integer transactionSequence) {
        this.transactionSequence = transactionSequence;
    }


    /**
     *
     **/
    private TGlobalTransactionProcessStatus status;

    public TGlobalTransactionProcessStatus getStatus() {
        return this.status;
    }

    public void setStatus(TGlobalTransactionProcessStatus status) {
        this.status = status;
    }


    /**
     *
     **/
    private TGlobalTransactionProcessExpectedStatus expectedStatus;

    public TGlobalTransactionProcessExpectedStatus getExpectedStatus() {
        return this.expectedStatus;
    }

    public void setExpectedStatus(TGlobalTransactionProcessExpectedStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }


    /**
     *
     **/
    private String serviceName;

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }


    /**
     *
     **/
    private String versionName;

    public String getVersionName() {
        return this.versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }


    /**
     *
     **/
    private String methodName;

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


    /**
     *
     **/
    private String rollbackMethodName;

    public String getRollbackMethodName() {
        return this.rollbackMethodName;
    }

    public void setRollbackMethodName(String rollbackMethodName) {
        this.rollbackMethodName = rollbackMethodName;
    }


    /**
     *
     **/
    private String requestJson;

    public String getRequestJson() {
        return this.requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }


    /**
     *
     **/
    private String responseJson;

    public String getResponseJson() {
        return this.responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }


    /**
     * @datatype(name="date")
     **/
    private Integer redoTimes;

    public Integer getRedoTimes() {
        return this.redoTimes;
    }

    public void setRedoTimes(Integer redoTimes) {
        this.redoTimes = redoTimes;
    }


    /**
     * @datatype(name="date")
     **/
    private java.util.Date nextRedoTime;

    public java.util.Date getNextRedoTime() {
        return this.nextRedoTime;
    }

    public void setNextRedoTime(java.util.Date nextRedoTime) {
        this.nextRedoTime = nextRedoTime;
    }


    /**
     * @datatype(name="date")
     **/
    private java.util.Date createdAt;

    public java.util.Date getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createdAt = createdAt;
    }


    /**
     * @datatype(name="date")
     **/
    private java.util.Date updatedAt;

    public java.util.Date getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updatedAt = updatedAt;
    }


    /**
     *
     **/
    private Integer createdBy;

    public Integer getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }


    /**
     *
     **/
    private Integer updatedBy;

    public Integer getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"").append("id").append("\":").append(this.id).append(",");
        stringBuilder.append("\"").append("transactionId").append("\":").append(this.transactionId).append(",");
        stringBuilder.append("\"").append("transactionSequence").append("\":").append(this.transactionSequence).append(",");
        stringBuilder.append("\"").append("status").append("\":").append(this.status).append(",");
        stringBuilder.append("\"").append("expectedStatus").append("\":").append(this.expectedStatus).append(",");
        stringBuilder.append("\"").append("serviceName").append("\":\"").append(this.serviceName).append("\",");
        stringBuilder.append("\"").append("versionName").append("\":\"").append(this.versionName).append("\",");
        stringBuilder.append("\"").append("methodName").append("\":\"").append(this.methodName).append("\",");
        stringBuilder.append("\"").append("rollbackMethodName").append("\":\"").append(this.rollbackMethodName).append("\",");
        stringBuilder.append("\"").append("requestJson").append("\":\"").append(this.requestJson).append("\",");
        stringBuilder.append("\"").append("responseJson").append("\":\"").append(this.responseJson).append("\",");
        stringBuilder.append("\"").append("redoTimes").append("\":").append(this.redoTimes).append(",");
        stringBuilder.append("\"").append("nextRedoTime").append("\":").append(this.nextRedoTime).append(",");
        stringBuilder.append("\"").append("createdAt").append("\":").append(this.createdAt).append(",");
        stringBuilder.append("\"").append("updatedAt").append("\":").append(this.updatedAt).append(",");
        stringBuilder.append("\"").append("createdBy").append("\":").append(this.createdBy).append(",");
        stringBuilder.append("\"").append("updatedBy").append("\":").append(this.updatedBy).append(",");

        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
      