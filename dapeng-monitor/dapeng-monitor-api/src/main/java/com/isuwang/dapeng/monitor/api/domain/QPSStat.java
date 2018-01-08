package com.github.dapeng.monitor.api.domain;

/**
 * QPS Stat
 **/
public class QPSStat {

    /**
     * 时间间隔:单位秒
     **/
    public Integer period;

    public Integer getPeriod() {
        return this.period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }


    /**
     * 统计分析时间(时间戳)
     **/
    public Long analysisTime;

    public Long getAnalysisTime() {
        return this.analysisTime;
    }

    public void setAnalysisTime(Long analysisTime) {
        this.analysisTime = analysisTime;
    }


    /**
     * 服务器IP
     **/
    public String serverIP;

    public String getServerIP() {
        return this.serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }


    /**
     * 服务器端口
     **/
    public Integer serverPort;

    public Integer getServerPort() {
        return this.serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }


    /**
     * 调用次数
     **/
    public Integer callCount;

    public Integer getCallCount() {
        return this.callCount;
    }

    public void setCallCount(Integer callCount) {
        this.callCount = callCount;
    }


    /**
     * 服务名称
     **/
    public String serviceName;

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }


    /**
     * 方法名称
     **/
    public String methodName;

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


    /**
     * 版本号
     **/
    public String versionName;

    public String getVersionName() {
        return this.versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"").append("period").append("\":").append(this.period).append(",");
        stringBuilder.append("\"").append("analysisTime").append("\":").append(this.analysisTime).append(",");
        stringBuilder.append("\"").append("serverIP").append("\":\"").append(this.serverIP).append("\",");
        stringBuilder.append("\"").append("serverPort").append("\":").append(this.serverPort).append(",");
        stringBuilder.append("\"").append("callCount").append("\":").append(this.callCount).append(",");
        stringBuilder.append("\"").append("serviceName").append("\":\"").append(this.serviceName).append("\",");
        stringBuilder.append("\"").append("methodName").append("\":\"").append(this.methodName).append("\",");
        stringBuilder.append("\"").append("versionName").append("\":\"").append(this.versionName).append("\",");

        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
      