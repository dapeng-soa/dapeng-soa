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

import com.google.gson.Gson;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.org.apache.thrift.TException;

import java.util.Date;

/**
 * Soa Transactional Process Template
 *
 * @author craneding
 * @date 16/4/11
 */
public class GlobalTransactionProcessTemplate<REQ> {

    private REQ req;

    public GlobalTransactionProcessTemplate(REQ req) {
        this.req = req;
    }

    public <T> T execute(GlobalTransactionCallback<T> action) throws TException {
        final GlobalTransactionProcessService service = GlobalTransactionFactory.getGlobalTransactionProcessService();

        TGlobalTransactionProcess transactionProcess = null;

        boolean success = false, unknown = false;
        T result = null;

        try {
            InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
            TransactionContext transactionContext = TransactionContext.Factory.currentInstance();

            transactionContext.currentTransactionSequence(transactionContext.currentTransactionSequence() + 1);

            invocationContext.transactionId(transactionContext.currentTransactionId());
            invocationContext.transactionSequence(transactionContext.currentTransactionSequence());

            transactionProcess = new TGlobalTransactionProcess();
            transactionProcess.setCreatedAt(new Date());
            transactionProcess.setCreatedBy(0);
            transactionProcess.setExpectedStatus(TGlobalTransactionProcessExpectedStatus.Success);

            transactionProcess.setServiceName(invocationContext.serviceName());
            transactionProcess.setMethodName(invocationContext.methodName());
            transactionProcess.setVersionName(invocationContext.versionName());
            transactionProcess.setRollbackMethodName(invocationContext.methodName() + "_rollback");

            transactionProcess.setRequestJson(req == null ? "" : new Gson().toJson(req));
            transactionProcess.setResponseJson("");

            transactionProcess.setStatus(TGlobalTransactionProcessStatus.New);
            transactionProcess.setTransactionId(transactionContext.currentTransactionId());
            transactionProcess.setTransactionSequence(transactionContext.currentTransactionSequence());

            transactionProcess.setRedoTimes(0);
            transactionProcess.setNextRedoTime(new Date(System.currentTimeMillis() + 30 * 1000));

            transactionProcess = service.create(transactionProcess);

            result = action.doInTransaction();

            success = true;

            return result;
        } catch (SoaException e) {
            switch (e.getCode()) {
                case "AA98":// 连接失败
                    unknown = false;
                    break;
                case "AA96":// 超时
                case "9999":// 未知
                    unknown = true;
                    break;
                default:// 明确错误
                    unknown = false;
                    break;
            }

            throw e;
        } finally {
            final TGlobalTransactionProcessStatus status = success ? TGlobalTransactionProcessStatus.Success : (unknown ? TGlobalTransactionProcessStatus.Unknown : TGlobalTransactionProcessStatus.Fail);

            if (transactionProcess.getId() != null) {
                service.update(transactionProcess.getId(), result == null ? "" : new Gson().toJson(result), status);
            }
        }
    }

}
