package com.github.dapeng.transaction.dao;

import com.github.dapeng.transaction.api.domain.TGlobalTransaction;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;

import java.util.Date;
import java.util.List;

/**
 * Created by tangliu on 17/7/28.
 */
public interface ITransactionDao {

    /**
     * 插入全局事务记录
     *
     * @param globalTransaction
     * @return
     */
    Integer insert(TGlobalTransaction globalTransaction);

    /**
     * 插入事务过程记录
     *
     * @param globalTransactionProcess
     * @return
     */
    Integer insert(TGlobalTransactionProcess globalTransactionProcess);

    TGlobalTransaction getGlobalByIdForUpdate(Integer id);

    TGlobalTransactionProcess getProcessByIdForUpdate(Integer id);

    /**
     * 查找所有的失败的或者未知的事务过程记录
     *
     * @param transactionId
     * @return
     */
    List<TGlobalTransactionProcess> findFailedProcess(Integer transactionId);

    /**
     * 查找所有的成功的或者未知的事务过程记录
     *
     * @param transactionId
     * @return
     */
    List<TGlobalTransactionProcess> findSucceedProcess(Integer transactionId);

    /**
     * 查询所有失败，或者部分回滚的全局事务记录
     *
     * @return
     */
    List<TGlobalTransaction> findFailedGlobals();

    /**
     * 查找所有状态为成功，但子过程中有失败的全局事务记录
     *
     * @return
     */
    List<TGlobalTransaction> findSuccessWithFailedProcessGlobals();

    /**
     * 更新重试次数和下次重试时间
     *
     * @param id
     * @param redoTimes
     * @param nextRedoTime
     */
    void updateProcessRollbackTime(Integer id, Integer redoTimes, Date nextRedoTime);

    /**
     * 更新全局事务记录的状态和当前子事务序号
     *
     * @param status
     * @param currSequence
     * @param id
     */
    void updateGlobalTransactionStatusAndCurrSeq(Integer status, Integer currSequence, Integer id);

    /**
     * 更新事务过程的状态和返回结果
     *
     * @param id
     * @param status
     * @param response
     */
    void updateProcess(Integer id, Integer status, String response);

    /**
     * 更新事务过程的期望状态
     *
     * @param id
     * @param expectedStatus
     */
    void updateProcessExpectedStatus(Integer id, Integer expectedStatus);

}
