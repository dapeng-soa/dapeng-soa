package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.TransactionContext;

import java.util.Optional;

/**
 * Response wrapper
 * @author Ever
 */
public class SoaResponseWrapper {
    /**
     * TransactionContext
     */
    final TransactionContext transactionContext;
    /**
     * response message if exists
     */
    final Optional<Object> result;
    /**
     * codec for the response
     */
    final Optional<BeanSerializer> serializer;

    public SoaResponseWrapper(TransactionContext transactionContext, Optional<Object> result, Optional<BeanSerializer> serializer) {

        this.transactionContext = transactionContext;
        this.result = result;
        this.serializer = serializer;
    }
}
