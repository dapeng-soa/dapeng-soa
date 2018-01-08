package com.github.dapeng.message.consumer.api.annotation;

import java.lang.annotation.*;

/**
 * Created by tangliu on 2016/8/3.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MessageConsumer {

    String groupId() default "";

    String zkHost() default "127.0.0.1";

}
