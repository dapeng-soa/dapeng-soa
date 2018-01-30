package com.github.dapeng.monitor.domain;

/**
 * 服务处理数据
 **/
public class ServiceProcessData {

    /**
     * 时间间隔:单位分钟
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
     * 平台最小耗时(单位:毫秒)
     **/
    public Long pMinTime;

    public Long getPMinTime() {
        return this.pMinTime;
    }

    public void setPMinTime(Long pMinTime) {
        this.pMinTime = pMinTime;
    }


    /**
     * 平台最大耗时(单位:毫秒)
     **/
    public Long pMaxTime;

    public Long getPMaxTime() {
        return this.pMaxTime;
    }

    public void setPMaxTime(Long pMaxTime) {
        this.pMaxTime = pMaxTime;
    }


    /**
     * 平台平均耗时(单位:毫秒)
     **/
    public Long pAverageTime;

    public Long getPAverageTime() {
        return this.pAverageTime;
    }

    public void setPAverageTime(Long pAverageTime) {
        this.pAverageTime = pAverageTime;
    }


    /**
     * 平台总耗时(单位:毫秒)
     **/
    public Long pTotalTime;

    public Long getPTotalTime() {
        return this.pTotalTime;
    }

    public void setPTotalTime(Long pTotalTime) {
        this.pTotalTime = pTotalTime;
    }


    /**
     * 接口服务最小耗时(单位:毫秒)
     **/
    public Long iMinTime;

    public Long getIMinTime() {
        return this.iMinTime;
    }

    public void setIMinTime(Long iMinTime) {
        this.iMinTime = iMinTime;
    }


    /**
     * 接口服务最大耗时(单位:毫秒)
     **/
    public Long iMaxTime;

    public Long getIMaxTime() {
        return this.iMaxTime;
    }

    public void setIMaxTime(Long iMaxTime) {
        this.iMaxTime = iMaxTime;
    }


    /**
     * 接口服务平均耗时(单位:毫秒)
     **/
    public Long iAverageTime;

    public Long getIAverageTime() {
        return this.iAverageTime;
    }

    public void setIAverageTime(Long iAverageTime) {
        this.iAverageTime = iAverageTime;
    }


    /**
     * 接口服务总耗时(单位:毫秒)
     **/
    public Long iTotalTime;

    public Long getITotalTime() {
        return this.iTotalTime;
    }

    public void setITotalTime(Long iTotalTime) {
        this.iTotalTime = iTotalTime;
    }


    /**
     * 总调用次数
     **/
    public Integer totalCalls;

    public Integer getTotalCalls() {
        return this.totalCalls;
    }

    public void setTotalCalls(Integer totalCalls) {
        this.totalCalls = totalCalls;
    }


    /**
     * 成功调用次数
     **/
    public Integer succeedCalls;

    public Integer getSucceedCalls() {
        return this.succeedCalls;
    }

    public void setSucceedCalls(Integer succeedCalls) {
        this.succeedCalls = succeedCalls;
    }


    /**
     * 失败调用次数
     **/
    public Integer failCalls;

    public Integer getFailCalls() {
        return this.failCalls;
    }

    public void setFailCalls(Integer failCalls) {
        this.failCalls = failCalls;
    }


    /**
     * 请求的流量(单位:字节)
     **/
    public Integer requestFlow;

    public Integer getRequestFlow() {
        return this.requestFlow;
    }

    public void setRequestFlow(Integer requestFlow) {
        this.requestFlow = requestFlow;
    }


    /**
     * 响应的流量(单位:字节)
     **/
    public Integer responseFlow;

    public Integer getResponseFlow() {
        return this.responseFlow;
    }

    public void setResponseFlow(Integer responseFlow) {
        this.responseFlow = responseFlow;
    }


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"").append("period").append("\":").append(this.period).append(",");
        stringBuilder.append("\"").append("analysisTime").append("\":").append(this.analysisTime).append(",");
        stringBuilder.append("\"").append("serviceName").append("\":\"").append(this.serviceName).append("\",");
        stringBuilder.append("\"").append("methodName").append("\":\"").append(this.methodName).append("\",");
        stringBuilder.append("\"").append("versionName").append("\":\"").append(this.versionName).append("\",");
        stringBuilder.append("\"").append("serverIP").append("\":\"").append(this.serverIP).append("\",");
        stringBuilder.append("\"").append("serverPort").append("\":").append(this.serverPort).append(",");
        stringBuilder.append("\"").append("pMinTime").append("\":").append(this.pMinTime).append(",");
        stringBuilder.append("\"").append("pMaxTime").append("\":").append(this.pMaxTime).append(",");
        stringBuilder.append("\"").append("pAverageTime").append("\":").append(this.pAverageTime).append(",");
        stringBuilder.append("\"").append("pTotalTime").append("\":").append(this.pTotalTime).append(",");
        stringBuilder.append("\"").append("iMinTime").append("\":").append(this.iMinTime).append(",");
        stringBuilder.append("\"").append("iMaxTime").append("\":").append(this.iMaxTime).append(",");
        stringBuilder.append("\"").append("iAverageTime").append("\":").append(this.iAverageTime).append(",");
        stringBuilder.append("\"").append("iTotalTime").append("\":").append(this.iTotalTime).append(",");
        stringBuilder.append("\"").append("totalCalls").append("\":").append(this.totalCalls).append(",");
        stringBuilder.append("\"").append("succeedCalls").append("\":").append(this.succeedCalls).append(",");
        stringBuilder.append("\"").append("failCalls").append("\":").append(this.failCalls).append(",");
        stringBuilder.append("\"").append("requestFlow").append("\":").append(this.requestFlow).append(",");
        stringBuilder.append("\"").append("responseFlow").append("\":").append(this.responseFlow).append(",");

        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
      