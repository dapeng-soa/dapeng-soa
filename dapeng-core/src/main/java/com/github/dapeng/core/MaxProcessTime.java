package com.github.dapeng.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 慢服务最大时间
 *
 * @author huyj
 * @Created 2018-09-05 12:48
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxProcessTime {

    long maxTime() default 3000L;
}
