package com.github.dapeng.message.consumer.api.annotation;

import java.lang.annotation.*;

/**
 * Created by tangliu on 2016/8/3.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MessageConsumerAction {

    String topic() default "Binlog";

    String eventType();

}
