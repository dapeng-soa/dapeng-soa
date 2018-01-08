
package com.github.dapeng.transaction.api.service;

import com.github.dapeng.core.Processor;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.transaction.api.domain.TGlobalTransaction;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionsStatus;

/**
 *
 **/
@Service(name="com.github.dapeng.transaction.api.service.GlobalTransactionService" ,version = "1.0.0")
@Processor(className = "com.github.dapeng.transaction.api.GlobalTransactionServiceCodec$Processor")
public interface GlobalTransactionService {

    /**
     *
     **/
    TGlobalTransaction create(TGlobalTransaction globalTransaction) throws SoaException;

    /**
     *
     **/
    void update(Integer globalTransactionId, Integer currSequence, TGlobalTransactionsStatus status) throws SoaException;

}
        