package com.github.dapeng.core;


import java.lang.annotation.*;

/**
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Invocation {

    long timeout() default 2000;
}
