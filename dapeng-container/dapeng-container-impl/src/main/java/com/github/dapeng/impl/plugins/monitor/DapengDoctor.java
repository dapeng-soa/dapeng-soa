package com.github.dapeng.impl.plugins.monitor;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.healthcheck.Doctor;
import com.github.dapeng.api.healthcheck.ServiceHealthStatus;
import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.google.common.base.Joiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: zhup
 * @Date: 2018/8/2 11:07
 */

public  class DapengDoctor implements Doctor {


    private Map<String, String> map = new HashMap<>(16);

    DapengDoctor() {
    }

    @Override
    public void report(ServiceHealthStatus status, String remark, Class<?> serviceClass) {
        Class[] clazz = serviceClass.getInterfaces();
        map.put(clazz[0].getName(), Joiner.on("|").join(status.name(), remark));
    }


    @Override
    public Map<String, Object> diagnoseReport() {
        Map<String, Object> diagnoseMap = new HashMap<>(16);
        Map tasksInfo = mapTasksInfo();
        Map gcInfo = mapGCInfo();
        Map flowsInfo = mapFlows();
        Map errorsInfo = mapErrors();
        diagnoseMap.put("tasks", tasksInfo);
        diagnoseMap.put("gcInfos", gcInfo);
        diagnoseMap.put("flows", flowsInfo);
        diagnoseMap.put("errors", errorsInfo);
        diagnoseMap.put("serviceInfo", map);
        return diagnoseMap;
    }


    /**
     * 当前任务数
     *
     * @return
     */
    private Map<String, Object> mapTasksInfo() {
        Map<String, Object> taskMap = new HashMap<>(4);
        Container container = ContainerFactory.getContainer();
        ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) container.getDispatcher();
        taskMap.put("waitingQueue", poolExecutor.getQueue().size());
        taskMap.put("succeed", poolExecutor.getCompletedTaskCount());
        taskMap.put("total", poolExecutor.getTaskCount());
        return taskMap;
    }

    /**
     * GC信息
     *
     * @return
     */
    private Map<String, String> mapGCInfo() {
        Map<String, String> gdMap = new HashMap<>(4);
        //todo gc待实现
        gdMap.put("gcInfos", "0/0");
        return gdMap;
    }

    /**
     * flow信息
     *
     * @return
     */
    private Map<String, Object> mapFlows() {
        Map<String, Object> flowMap = new HashMap<>(8);
        List<DataPoint> dataPointsList = ServerCounterContainer.getInstance().invokePointsOfLastMinute();
        if (dataPointsList.size() > 0) {
            DataPoint dataPoint = dataPointsList.get(dataPointsList.size() - 1);
            Map<String, Long> lastMinuteMap = dataPoint.values();
            flowMap.put("lastMinuteMax", lastMinuteMap.get("max_request_flow"));
            flowMap.put("lastMinuteMin", lastMinuteMap.get("min_request_flow"));
            flowMap.put("lastMinuteAvg", lastMinuteMap.get("avg_request_flow"));
        }
        //todo 当天最大数
        return flowMap;
    }

    /**
     * 错误条目
     *
     * @return
     */
    private Map<String, Object> mapErrors() {
        Map<String, Object> errorsMap = new HashMap<>(8);
        return errorsMap;
    }
}
