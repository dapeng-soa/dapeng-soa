package com.github.dapeng.monitor.api.domain;

import java.util.Optional;

/**
 * DataSource Stat
 **/
public class DataSourceStat {

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
     * 连接地址
     **/
    public String url;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    /**
     * 用户名
     **/
    public String userName;

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    /**
     * 编号
     **/
    public String identity;

    public String getIdentity() {
        return this.identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }


    /**
     * 数据库类型
     **/
    public String dbType;

    public String getDbType() {
        return this.dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }


    /**
     * 池中连接数
     **/
    public Integer poolingCount;

    public Integer getPoolingCount() {
        return this.poolingCount;
    }

    public void setPoolingCount(Integer poolingCount) {
        this.poolingCount = poolingCount;
    }


    /**
     * 池中连接数峰值
     **/
    public Optional<Integer> poolingPeak = Optional.empty();

    public Optional<Integer> getPoolingPeak() {
        return this.poolingPeak;
    }

    public void setPoolingPeak(Optional<Integer> poolingPeak) {
        this.poolingPeak = poolingPeak;
    }


    /**
     * 池中连接数峰值时间
     **/
    public Optional<Long> poolingPeakTime = Optional.empty();

    public Optional<Long> getPoolingPeakTime() {
        return this.poolingPeakTime;
    }

    public void setPoolingPeakTime(Optional<Long> poolingPeakTime) {
        this.poolingPeakTime = poolingPeakTime;
    }


    /**
     * 活跃连接数
     **/
    public Integer activeCount;

    public Integer getActiveCount() {
        return this.activeCount;
    }

    public void setActiveCount(Integer activeCount) {
        this.activeCount = activeCount;
    }


    /**
     * 活跃连接数峰值
     **/
    public Optional<Integer> activePeak = Optional.empty();

    public Optional<Integer> getActivePeak() {
        return this.activePeak;
    }

    public void setActivePeak(Optional<Integer> activePeak) {
        this.activePeak = activePeak;
    }


    /**
     * 活跃连接数峰值时间
     **/
    public Optional<Long> activePeakTime = Optional.empty();

    public Optional<Long> getActivePeakTime() {
        return this.activePeakTime;
    }

    public void setActivePeakTime(Optional<Long> activePeakTime) {
        this.activePeakTime = activePeakTime;
    }


    /**
     * 执行数
     **/
    public Integer executeCount;

    public Integer getExecuteCount() {
        return this.executeCount;
    }

    public void setExecuteCount(Integer executeCount) {
        this.executeCount = executeCount;
    }


    /**
     * 错误数
     **/
    public Integer errorCount;

    public Integer getErrorCount() {
        return this.errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"").append("period").append("\":").append(this.period).append(",");
        stringBuilder.append("\"").append("analysisTime").append("\":").append(this.analysisTime).append(",");
        stringBuilder.append("\"").append("serverIP").append("\":\"").append(this.serverIP).append("\",");
        stringBuilder.append("\"").append("serverPort").append("\":").append(this.serverPort).append(",");
        stringBuilder.append("\"").append("url").append("\":\"").append(this.url).append("\",");
        stringBuilder.append("\"").append("userName").append("\":\"").append(this.userName).append("\",");
        stringBuilder.append("\"").append("identity").append("\":\"").append(this.identity).append("\",");
        stringBuilder.append("\"").append("dbType").append("\":\"").append(this.dbType).append("\",");
        stringBuilder.append("\"").append("poolingCount").append("\":").append(this.poolingCount).append(",");
        stringBuilder.append("\"").append("poolingPeak").append("\":").append(this.poolingPeak.isPresent() ? this.poolingPeak.get() : null).append(",");
        stringBuilder.append("\"").append("poolingPeakTime").append("\":").append(this.poolingPeakTime.isPresent() ? this.poolingPeakTime.get() : null).append(",");
        stringBuilder.append("\"").append("activeCount").append("\":").append(this.activeCount).append(",");
        stringBuilder.append("\"").append("activePeak").append("\":").append(this.activePeak.isPresent() ? this.activePeak.get() : null).append(",");
        stringBuilder.append("\"").append("activePeakTime").append("\":").append(this.activePeakTime.isPresent() ? this.activePeakTime.get() : null).append(",");
        stringBuilder.append("\"").append("executeCount").append("\":").append(this.executeCount).append(",");
        stringBuilder.append("\"").append("errorCount").append("\":").append(this.errorCount).append(",");

        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
      