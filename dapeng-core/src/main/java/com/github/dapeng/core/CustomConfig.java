package com.github.dapeng.core;

import java.lang.annotation.*;

/**
 * 注解 自定义 配置
 *
 * @author maple, struy
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomConfig {
    long timeout() default 1000L;

    String loadBalance() default "random";

}
