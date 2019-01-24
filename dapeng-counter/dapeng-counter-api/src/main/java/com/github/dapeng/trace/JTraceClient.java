package com.github.dapeng.trace;

import com.github.dapeng.basic.api.counter.domain.DataPoint;

import java.util.List;
import java.util.Map;

/**
 * 数据埋点上报处理类
 *
 * @author huyj
 * @Created 2018-12-24 15:51
 */
public class JTraceClient {
    /**
     * 单点提交
     * 该数据有多个tag，多个value
     * monitorId建议为:serviceName.bizName
     **/
    public void trace(String monitorId, Map<String, String> tags, Map<String, Long> values) {
        TraceReportHandler.getInstance().appendPoint(new DataPoint().bizTag(monitorId).tags(tags).values(values));
    }

    /**
     * 单点提交
     **/
    public void tracePoint(DataPoint dataPoint) {
        TraceReportHandler.getInstance().appendPoint(dataPoint);
    }

    /**
     * 批量提交
     **/
    public void tracePoints(List<DataPoint> dataPoints) {
        TraceReportHandler.getInstance().appendPoints(dataPoints);
    }

    /**
     * 刷新缓存队列 ,上报数据
     **/
    public void flush() {
        TraceReportHandler.getInstance().flush();
    }

}
