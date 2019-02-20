namespace java com.github.dapeng.scheduler.events

include 'task_result_enum.thrift'

/**
* 定时任务event
**/
struct TaskEvent {
    /**
        * 事件Id
        **/
        1: i64 id,

    /**
        * 服务名
        **/
        2: string serviceName,

    /**
        * 方法名
        **/
        3: string methodName,

    /**
        * 版本号
        **/
        4: string version,


    /**
        * 耗时
        **/
        5: i64 costTime,

  /**
    * SUCCEED = 1:执行成功 ;FAIL = 2:执行失败
    **/
    6: task_result_enum.TaskStatusEnum taskStatus,

    /**
        * 备注
        **/
        7: string remark
}