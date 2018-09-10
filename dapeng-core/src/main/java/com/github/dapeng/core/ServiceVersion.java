package com.github.dapeng.core;

import java.lang.annotation.*;

/**
 * 服务实现版本号
 *
 * @author huyj
 * @Created 2018-07-27 11:55
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceVersion {
    String version() default "1.0.0";
    boolean isRegister() default true;
}
