namespace java com.github.dapeng.soa.monitor.api.domain

/**
* QPS Stat
**/
struct QPSStat {

    /**
    * 时间间隔:单位秒
    **/
    1:i32 period,

    /**
    * 统计分析时间(时间戳)
    **/
    2:i64 analysisTime,

    /**
    * 服务器IP
    **/
    3:string serverIP,

    /**
    * 服务器端口
    **/
    4:i32 serverPort,

    /**
    * 调用次数
    **/
    5:i32 callCount,

    /**
    * 服务名称
    **/
    6:string serviceName,

    /**
    * 方法名称
    **/
    7:string methodName,

    /**
    * 版本号
    **/
    8:string versionName

}

/**
* 平台处理数据
**/
struct PlatformProcessData {

    /**
    * 时间间隔:单位分钟
    **/
    1:i32 period,

    /**
    * 统计分析时间(时间戳)
    **/
    2:i64 analysisTime,

    /**
    * 服务名称
    **/
    3:string serviceName,

    /**
    * 方法名称
    **/
    4:string methodName,

    /**
    * 版本号
    **/
    5:string versionName,

    /**
    * 服务器IP
    **/
    6:string serverIP,

    /**
    * 服务器端口
    **/
    7:i32 serverPort,

    /**
    * 平台最小耗时(单位:毫秒)
    **/
    8:i64 pMinTime,

    /**
    * 平台最大耗时(单位:毫秒)
    **/
    9:i64 pMaxTime,

    /**
    * 平台平均耗时(单位:毫秒)
    **/
    10:i64 pAverageTime,

    /**
    * 平台总耗时(单位:毫秒)
    **/
    11:i64 pTotalTime,

    /**
    * 接口服务最小耗时(单位:毫秒)
    **/
    12:i64 iMinTime,

    /**
    * 接口服务最大耗时(单位:毫秒)
    **/
    13:i64 iMaxTime,

    /**
    * 接口服务平均耗时(单位:毫秒)
    **/
    14:i64 iAverageTime,

    /**
    * 接口服务总耗时(单位:毫秒)
    **/
    15:i64 iTotalTime,

    /**
    * 总调用次数
    **/
    16:i32 totalCalls,

    /**
    * 成功调用次数
    **/
    17:i32 succeedCalls,

    /**
    * 失败调用次数
    **/
    18:i32 failCalls,

    /**
    * 请求的流量(单位:字节)
    **/
    19:i32 requestFlow,

    /**
    * 响应的流量(单位:字节)
    **/
    20:i32 responseFlow

}

/**
* DataSource Stat
**/
struct DataSourceStat {

    /**
    * 时间间隔:单位秒
    **/
    1:i32 period,

    /**
    * 统计分析时间(时间戳)
    **/
    2:i64 analysisTime,

    /**
    * 服务器IP
    **/
    3:string serverIP,

    /**
    * 服务器端口
    **/
    4:i32 serverPort,

    /**
    * 连接地址
    **/
    5:string url,

    /**
    * 用户名
    **/
    6:string userName,

    /**
    * 编号
    **/
    7:string identity,

    /**
    * 数据库类型
    **/
    8:string dbType,

    /**
    * 池中连接数
    **/
    9:i32 poolingCount,

    /**
    * 池中连接数峰值
    **/
    10:optional i32 poolingPeak,

    /**
    * 池中连接数峰值时间
    **/
    11:optional i64 poolingPeakTime,

    /**
    * 活跃连接数
    **/
    13:i32 activeCount,

    /**
    * 活跃连接数峰值
    **/
    14:optional i32 activePeak,

    /**
    * 活跃连接数峰值时间
    **/
    15:optional i64 activePeakTime,

    /**
    * 执行数
    **/
    16:i32 executeCount,

    /**
    * 错误数
    **/
    17:i32 errorCount
}