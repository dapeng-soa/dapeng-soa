package com.github.dapeng.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: zhup
 * @Date: 2019/1/17 2:22 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TCC {


    String confirmMethod();

    String cancelMethod();

    boolean asynCC();
}
