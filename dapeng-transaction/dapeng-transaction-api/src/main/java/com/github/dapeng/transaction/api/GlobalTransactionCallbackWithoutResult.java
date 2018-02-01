package com.github.dapeng.transaction.api;

import com.github.dapeng.org.apache.thrift.TException;

/**
 * Soa Transactional ProcessCallback WithoutResult
 *
 * @author craneding
 * @date 16/4/11
 */
public abstract class GlobalTransactionCallbackWithoutResult implements GlobalTransactionCallback<Object> {

    @Override
    public Object doInTransaction() throws TException {
        doInTransactionWithoutResult();

        return null;
    }

    protected abstract void doInTransactionWithoutResult() throws TException;

}
