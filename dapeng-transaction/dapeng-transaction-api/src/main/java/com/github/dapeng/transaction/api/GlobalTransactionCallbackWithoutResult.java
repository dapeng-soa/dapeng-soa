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
    public boolean doInTransaction() throws TException {
        doInTransactionWithResult();

        return doInTransactionWithResult();
    }

    protected abstract boolean doInTransactionWithResult() throws TException;

}
