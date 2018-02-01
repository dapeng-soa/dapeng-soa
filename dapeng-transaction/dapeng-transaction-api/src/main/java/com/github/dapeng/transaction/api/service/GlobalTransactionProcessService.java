
package com.github.dapeng.transaction.api.service;

import com.github.dapeng.core.SoaBaseCodeInterface;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.Processor;
import com.github.dapeng.core.Service;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;

import java.util.Date;

/**
 *
 **/
@Service(name="com.github.dapeng.transaction.api.service.GlobalTransactionProcessService", version = "1.0.0")
@Processor(className = "com.github.dapeng.transaction.api.GlobalTransactionProcessServiceCodec$Processor")
public interface GlobalTransactionProcessService {

    /**
     *
     **/
    TGlobalTransactionProcess create(TGlobalTransactionProcess globalTransactionProcess) throws SoaException;

    /**
     *
     **/
    void update(Integer globalTransactionProcessId, String responseJson, TGlobalTransactionProcessStatus status) throws SoaException;

    void updateExpectedStatus(Integer processId, TGlobalTransactionProcessExpectedStatus status) throws SoaException;

    void updateRedoTimes(Integer globalTransactionProcessId) throws SoaException;
}
        