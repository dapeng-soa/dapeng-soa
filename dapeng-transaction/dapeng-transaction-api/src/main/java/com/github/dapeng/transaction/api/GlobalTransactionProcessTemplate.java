package com.github.dapeng.transaction.api;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

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

            Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).create();

            transactionProcess.setRequestJson(req == null ? "" : gson.toJson(req));
            transactionProcess.setResponseJson("");

            transactionProcess.setStatus(TGlobalTransactionProcessStatus.New);
            transactionProcess.setTransactionId(transactionContext.currentTransactionId());
            transactionProcess.setTransactionSequence(transactionContext.currentTransactionSequence());

            transactionProcess.setRedoTimes(0);
            transactionProcess.setNextRedoTime(new Date(System.currentTimeMillis() + 30 * 1000));

            transactionProcess = service.create(transactionProcess);

            success = action.doInTransaction();

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
