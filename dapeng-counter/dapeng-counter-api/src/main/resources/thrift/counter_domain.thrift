namespace java com.github.dapeng.basic.api.counter.domain

/**
* 数据点
**/
struct DataPoint {
    /**
    * 业务类型, 在时序数据库中也叫metric/measurement, 相当于关系型数据库的数据表
    **/
    1: string bizTag,
    /**
     * field
     * values 可以为一个，可以为多个Field
     * value支持的类型floats，integers，strings，booleans
     * value会用于展示
     */
    2: map<string, i64> values,
    3: i64 timestamp,
    /**
     * tag是可选的。不过写入数据时最好加上tag，因为它可以被索引。tag的类型只能是字符串。
     */
    4: map<string,string> tags,
    5: string database,
}