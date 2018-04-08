package com.github.dapeng.core.helper;

import com.github.dapeng.core.*;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;

import java.util.Optional;

/**
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

        InvocationContextImpl.InvocationContextProxy invocationCtxProxy = InvocationContextImpl.Factory.getInvocationContextProxy();

        SoaHeader header = new SoaHeader();

        /**
         * 如果有invocationCtxProxy(一般在web或者三方系统)
         */
        if (invocationCtxProxy != null) {
            header.setSessionTid(invocationCtxProxy.sessionTid());
            header.setUserIp(invocationCtxProxy.userIp());
            header.setUserId(invocationCtxProxy.userId());
            header.setOperatorId(invocationCtxProxy.operatorId());

            header.setCallerMid(invocationCtxProxy.callerMid());
        }

        header.setServiceName(invocationContext.serviceName());
        header.setVersionName(invocationContext.versionName());
        header.setMethodName(invocationContext.methodName());

        header.setCallerMid(invocationContext.callerMid());
        header.setCallerIp(invocationContext.callerIp());
        header.setCallerPort(invocationContext.callerPort());
        header.setCallerTid(Optional.ofNullable(invocationContext.callerTid()));

        if (!header.getOperatorId().isPresent()) {
            header.setOperatorId(invocationContext.operatorId());
        }
        if (!header.getUserId().isPresent()) {
            header.setUserId(invocationContext.userId());
        }
        if (!header.getUserIp().isPresent()) {
            header.setUserIp(invocationContext.userIp());
        }
        if (!header.getSessionTid().isPresent()) {
            header.setSessionTid(invocationContext.sessionTid());
        }

        /**
         * 如果容器内调用其它服务, 将原始的调用者信息传递
         */
        if (TransactionContext.hasCurrentInstance()) {
            TransactionContext transactionContext = TransactionContext.Factory.currentInstance();
            SoaHeader oriHeader = transactionContext.getHeader();

            if (!header.getOperatorId().isPresent()) {
                header.setOperatorId(oriHeader.getOperatorId());
            }
            if (!header.getUserId().isPresent()) {
                header.setUserId(oriHeader.getUserId());
            }
            if (!header.getUserIp().isPresent()) {
                header.setUserIp(oriHeader.getUserIp());
            }

            // 传递tid
            header.setSessionTid(transactionContext.sessionTid());
            invocationContext.callerTid(transactionContext.calleeTid());
        }
        return header;
    }
}
