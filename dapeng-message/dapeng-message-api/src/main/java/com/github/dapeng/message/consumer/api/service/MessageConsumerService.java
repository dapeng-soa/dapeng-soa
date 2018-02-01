package com.github.dapeng.message.consumer.api.service;

import com.github.dapeng.message.consumer.api.context.ConsumerContext;

/**
 * Created by tangliu on 2016/9/12.
 */
public interface MessageConsumerService {
    void addConsumer(ConsumerContext context);

    void removeConsumer(ConsumerContext context);

    void clearConsumers();
}
