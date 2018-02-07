namespace java com.github.dapeng.basic.api.counter.domain

/**
* 数据点
**/
struct DataPoint {
    /**
    * 业务类型, 在时序数据库中也叫metric/measurement, 相当于关系型数据库的数据表
    **/
    1: string bizTag,
    2: string value,
    3: i64 timestamp,
    4: map<string,string> tags,
    5: string database,
}