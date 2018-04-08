package com.github.dapeng.core.helper;

import com.github.dapeng.core.*;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;

import java.util.Optional;

/**
 *
 * @author tangliu
 * @date 2016/5/16
 */
public class SoaHeaderHelper {

    /**
     * 服务端获取soaHeader
     *
     * @param setDefaultIfEmpty 是否需要设置默认的customer和operator，如果header中没有值
     * @return
     */
    public static SoaHeader getSoaHeader(boolean setDefaultIfEmpty) {
        TransactionContext context = TransactionContext.Factory.currentInstance();

        if (context.getHeader() == null) {
            SoaHeader header = new SoaHeader();
            context.setHeader(header);
        }

        if (setDefaultIfEmpty) {
            resetSoaHeader(context.getHeader());
        }

        return context.getHeader();
    }

    /**
     * 设置默认的customer和operator
     *
     * @param header
     */
    public static void resetSoaHeader(SoaHeader header) {
        //TODO
        if (!header.getOperatorId().isPresent()) {
            header.setOperatorId(Optional.of(0L));
        }
    }

    public static SoaHeader buildHeader() {
        InvocationContext invocationContext = InvocationContextImpl.Factory.getCurrentInstance();

        if (TransactionContext.hasCurrentInstance()) {

        }
        InvocationContextImpl.InvocationContextProxy invocationCtxProxy = InvocationContextImpl.Factory.getInvocationContextProxy();

        if (invocationContext != null) {

        }
        SoaHeader header = new SoaHeader();
        header.setServiceName(invocationContext.serviceName());
        header.setVersionName(invocationContext.versionName());
        header.setMethodName(invocationContext.methodName());

        header.setCallerMid(invocationContext.callerMid());
        header.setCallerIp(invocationContext.callerIp());
        header.setCallerPort(invocationContext.callerPort());
        header.setCallerTid(Optional.ofNullable(invocationContext.callerTid()));
        header.setOperatorId(invocationContext.operatorId());
        header.setUserId(invocationContext.userId());
        header.setUserIp(invocationContext.userIp());
        header.setSessionTid(invocationContext.sessionTid());

        return header;
    }
}
