package com.github.dapeng.impl.plugins.monitor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务处理数据
 **/
public class ServiceProcessData {

    /**
     * 时间间隔:秒
     **/
    private Integer period;

    public Integer getPeriod() {
        return this.period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }


    /**
     * 统计分析时间(时间戳)
     **/
    private Long analysisTime;

    public Long getAnalysisTime() {
        return this.analysisTime;
    }

    public void setAnalysisTime(Long analysisTime) {
        this.analysisTime = analysisTime;
    }


    /**
     * 服务名称
     **/
    private String serviceName;

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }


    /**
     * 方法名称
     **/
    private String methodName;

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


    /**
     * 版本号
     **/
    private String versionName;

    public String getVersionName() {
        return this.versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }


    /**
     * 服务器IP
     **/
    private String serverIP;

    public String getServerIP() {
        return this.serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }


    /**
     * 服务器端口
     **/
    private Integer serverPort;

    public Integer getServerPort() {
        return this.serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }



    /**
     * 接口服务最小耗时(单位:毫秒)
     **/
    private Long iMinTime;

    public Long getIMinTime() {
        return this.iMinTime;
    }

    public void setIMinTime(Long iMinTime) {
        this.iMinTime = iMinTime;
    }


    /**
     * 接口服务最大耗时(单位:毫秒)
     **/
    private Long iMaxTime;

    public Long getIMaxTime() {
        return this.iMaxTime;
    }

    public void setIMaxTime(Long iMaxTime) {
        this.iMaxTime = iMaxTime;
    }


    /**
     * 接口服务平均耗时(单位:毫秒)
     **/
    private Long iAverageTime;

    public Long getIAverageTime() {
        return this.iAverageTime;
    }

    public void setIAverageTime(Long iAverageTime) {
        this.iAverageTime = iAverageTime;
    }


    /**
     * 接口服务总耗时(单位:毫秒)
     **/
    private Long iTotalTime;

    public Long getITotalTime() {
        return this.iTotalTime;
    }

    public void setITotalTime(Long iTotalTime) {
        this.iTotalTime = iTotalTime;
    }


    /**
     * 总调用次数
     **/
    private AtomicInteger totalCalls = new AtomicInteger(0);

    public AtomicInteger getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(AtomicInteger totalCalls) {
        this.totalCalls = totalCalls;
    }


    /**
     * 成功调用次数
     **/
    private AtomicInteger succeedCalls = new AtomicInteger(0);

    public AtomicInteger getSucceedCalls() {
        return succeedCalls;
    }

    public void setSucceedCalls(AtomicInteger succeedCalls) {
        this.succeedCalls = succeedCalls;
    }

    /**
     * 失败调用次数
     **/
    private AtomicInteger failCalls = new AtomicInteger(0);

    public AtomicInteger getFailCalls() {
        return failCalls;
    }

    public void setFailCalls(AtomicInteger failCalls) {
        this.failCalls = failCalls;
    }
}
      