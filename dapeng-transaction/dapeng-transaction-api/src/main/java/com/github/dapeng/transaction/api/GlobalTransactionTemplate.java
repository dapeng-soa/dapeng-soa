/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.transaction.api;

import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.transaction.api.domain.TGlobalTransaction;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionsStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;
import com.github.dapeng.org.apache.thrift.TException;

import java.util.Date;

/**
 * Soa Transactional Process Template
 *
 * @author craneding
 * @date 16/4/11
 */
public class GlobalTransactionTemplate {

    public <T> T execute(GlobalTransactionCallback<T> action) throws TException {
        final GlobalTransactionService service = GlobalTransactionFactory.getGlobalTransactionService();
        final TransactionContext context = TransactionContext.Factory.currentInstance();

        boolean success = false;

        TGlobalTransaction globalTransaction = null;
        try {
            globalTransaction = new TGlobalTransaction();
            globalTransaction.setCreatedAt(new Date());
            globalTransaction.setCreatedBy(0);
            globalTransaction.setCurrSequence(0);
            globalTransaction.setStatus(TGlobalTransactionsStatus.New);

            globalTransaction = service.create(globalTransaction);

            context.currentTransactionSequence(0);
            context.currentTransactionId(globalTransaction.getId());

            T result = action.doInTransaction();

            success = true;

            return result;
        } finally {
            if (globalTransaction.getId() != null) {
                service.update(globalTransaction.getId(), context.currentTransactionSequence(), success ? TGlobalTransactionsStatus.Success : TGlobalTransactionsStatus.Fail);
            }
        }
    }

}
