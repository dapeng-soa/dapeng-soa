package com.github.dapeng.api;

/**
 * @author ever
 * @date 2018/07/26
 */
public interface DapengDoctor {
    /**
     * 返回服务的健康度, Json格式
     * {
     *    "status": "GREEN",
     *    "tasks": "10/1580/1590", #当前任务
     *    "fullGc": 
     * }
     * @return
     */
    String diagnoseReport();
}
