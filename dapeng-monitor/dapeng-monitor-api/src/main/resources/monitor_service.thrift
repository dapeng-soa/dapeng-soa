include "monitor_domain.thrift"

namespace java com.github.dapeng.soa.monitor.api.service

/**
* 监控服务
**/
service MonitorService {

    /**
    * 上送QPS信息
    **/
    void uploadQPSStat(1:list<monitor_domain.QPSStat> qpsStats),

    /**
    * 上送平台处理数据
    **/
    void uploadPlatformProcessData(1:list<monitor_domain.PlatformProcessData> platformProcessDatas),

    /**
    * 上送DataSource信息
    **/
    void uploadDataSourceStat(1:list<monitor_domain.DataSourceStat> dataSourceStat)

}
