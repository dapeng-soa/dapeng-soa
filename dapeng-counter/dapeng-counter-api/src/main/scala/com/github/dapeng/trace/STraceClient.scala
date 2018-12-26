package com.github.dapeng.trace

import com.github.dapeng.basic.api.counter.domain.DataPoint

import scala.collection.JavaConverters._

class STraceClient {
  /**
    * 单点提交
    * 该数据有多个tag，多个value
    * monitorId建议为:serviceName.bizName
    **/
  def trace(monitorId: String, tags: Map[String, String], values: Map[String, Long]): Unit = {
    val result: DataPoint = new DataPoint().bizTag(monitorId).tags(tags.asJava)
    result.setValues(values.map{case (a,b) => (a.toString, java.lang.Long.valueOf(b))}.asJava)
    TraceReportHandler.getInstance.appendPoint(result)
  }

  /**
    * 单点提交
    **/
  def tracePoint(dataPoint: DataPoint): Unit = {
    TraceReportHandler.getInstance.appendPoint(dataPoint)
  }

  /**
    * 批量提交
    **/
  def tracePoints(dataPoints: List[DataPoint]): Unit = {
    TraceReportHandler.getInstance.appendPoints(dataPoints.asJava)
  }

  /**
    * 刷新缓存队列 ,上报数据
    **/
  def flush(): Unit = {
    TraceReportHandler.getInstance.flush()
  }
}
