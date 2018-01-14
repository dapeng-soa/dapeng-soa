package com.github.dapeng.transaction.service;

import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.transaction.dao.ITransactionDao;
import com.github.dapeng.transaction.utils.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.github.dapeng.transaction.service.GlobalTransactionServiceImpl.checkout;

/**
 * Created by tangliu on 2016/4/12.
 */
@Transactional(value = "globalTransaction", rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
public class GlobalTransactionProcessServiceImpl implements GlobalTransactionProcessService {

    Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionProcessServiceImpl.class);

    @Autowired
    ITransactionDao transactionDao;

    @Override
    public TGlobalTransactionProcess create(TGlobalTransactionProcess gp) throws SoaException {


        checkout(gp.getTransactionId() != null, ErrorCode.INPUTERROR.getCode(), "transactionId不能为空");
        checkout(gp.getTransactionSequence() != null, ErrorCode.INPUTERROR.getCode(), "过程所属序列号不能为空");
        checkout(gp.getStatus() != null, ErrorCode.INPUTERROR.getCode(), "过程当前状态不能为空");
        checkout(gp.getExpectedStatus() != null, ErrorCode.INPUTERROR.getCode(), "过程目标状态不能为空");
        checkout(gp.getServiceName() != null, ErrorCode.INPUTERROR.getCode(), "服务名称不能为空");
        checkout(gp.getVersionName() != null, ErrorCode.INPUTERROR.getCode(), "服务版本不能为空");
        checkout(gp.getMethodName() != null, ErrorCode.INPUTERROR.getCode(), "方法名称不能为空");
        checkout(gp.getRollbackMethodName() != null, ErrorCode.INPUTERROR.getCode(), "回滚方法名称不能为空");
        checkout(gp.getRequestJson() != null, ErrorCode.INPUTERROR.getCode(), "过程请求参数Json序列化不能为空");
        checkout(gp.getResponseJson() != null, ErrorCode.INPUTERROR.getCode(), "过程响应参数Json序列化不能为空");

        Integer id = transactionDao.insert(gp);

        gp.setId(id);

        LOGGER.info("创建事务过程({}),({}),({}),({}),({}),({}),({}),({}),({}),({}),({})", gp.getId(), gp.getTransactionId(),
                gp.getTransactionSequence(), gp.getStatus().getValue(), gp.getExpectedStatus(), gp.getServiceName(), gp.getMethodName(), gp.getVersionName(),
                gp.getRollbackMethodName(), gp.getRequestJson(), gp.getResponseJson());

        return gp;
    }

    @Override
    public void update(Integer globalTransactionProcessId, String responseJson, TGlobalTransactionProcessStatus status) throws SoaException {

        TGlobalTransactionProcess process = transactionDao.getProcessByIdForUpdate(globalTransactionProcessId);

        if (process == null)
            throw new SoaException(ErrorCode.NOTEXIST.getCode(), ErrorCode.NOTEXIST.getMsg());

        LOGGER.info("更新事务过程({})前,状态({}),过程响应参数({})", process.getId(), process.getStatus(), process.getResponseJson());
        transactionDao.updateProcess(globalTransactionProcessId, status.getValue(), responseJson);
        LOGGER.info("更新事务过程({})后,状态({}),过程响应参数({})", process.getId(), status.getValue(), responseJson);
    }

    /**
     * 更新事务过程期望状态
     *
     * @param processId
     * @param status
     * @throws SoaException
     */
    @Override
    public void updateExpectedStatus(Integer processId, TGlobalTransactionProcessExpectedStatus status) throws SoaException {

        TGlobalTransactionProcess process = transactionDao.getProcessByIdForUpdate(processId);
        if (process == null)
            throw new SoaException(ErrorCode.NOTEXIST.getCode(), ErrorCode.NOTEXIST.getMsg());

        LOGGER.info("更新事务过程({})前,过程目标状态({})", process.getId(), process.getExpectedStatus().getValue());


        transactionDao.updateProcessExpectedStatus(processId, status.getValue());

        LOGGER.info("更新事务过程({})后,过程目标状态({})", process.getId(), status.getValue());
    }

    /**
     * 更新事务过程的重试次数和下次重试时间
     *
     * @param processId
     */
    @Override
    public void updateRedoTimes(Integer processId) throws SoaException {

        TGlobalTransactionProcess process = transactionDao.getProcessByIdForUpdate(processId);
        if (process == null)
            throw new SoaException(ErrorCode.NOTEXIST.getCode(), ErrorCode.NOTEXIST.getMsg());


        LOGGER.info("更新事务过程({})前,重试次数({}),下次重试时间({})", process.getId(), process.getRedoTimes(), new Date(process.getNextRedoTime().getTime()));

        process.setRedoTimes(process.getRedoTimes() + 1);
        process.setNextRedoTime(new Date(System.currentTimeMillis() + (30 * 1000)));

        transactionDao.updateProcessRollbackTime(processId, process.getRedoTimes(), process.getNextRedoTime());

        LOGGER.info("更新事务过程({})后,重试次数({}),下次重试时间({})", process.getId(), process.getRedoTimes(), new Date(process.getNextRedoTime().getTime()));
    }

}

