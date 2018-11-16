package com.github.dapeng.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author hui
 * @date 2018/11/7 0007 14:27
 */
public interface HealthCheck {
     /**
      * 业务方实现健康检查逻辑并上报的接口
      */
     HealthCheckResult checkReport();
}
