package com.github.dapeng.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that support tcc
 * @Author: zhup
 * @Date: 2019/1/17 2:22 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TCC {

    /**
     * confirm method for tcc
     * @return
     */
    String confirmMethod();

    /**
     * cancel method for tcc
     * @return
     */
    String cancelMethod();

    /**
     * invoke cc method async or not.
     * @return
     */
    boolean asynCC();
}
