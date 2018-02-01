package com.github.dapeng.monitor.api.service;

import com.github.dapeng.core.Processor;
import com.github.dapeng.core.Service;

/**
 * 监控服务
 **/
@Service(name="com.github.dapeng.monitor.api.service.MonitorService",version = "1.0.0")
@Processor(className = "com.github.dapeng.monitor.api.MonitorServiceCodec$Processor")
public interface MonitorService {

    /**
     * 上送QPS信息
     **/


    void uploadQPSStat(java.util.List<com.github.dapeng.monitor.api.domain.QPSStat> qpsStats) throws com.github.dapeng.core.SoaException;


    /**
     * 上送平台处理数据
     **/


    void uploadPlatformProcessData(java.util.List<com.github.dapeng.monitor.api.domain.PlatformProcessData> platformProcessDatas) throws com.github.dapeng.core.SoaException;


    /**
     * 上送DataSource信息
     **/


    void uploadDataSourceStat(java.util.List<com.github.dapeng.monitor.api.domain.DataSourceStat> dataSourceStat) throws com.github.dapeng.core.SoaException;


}
        