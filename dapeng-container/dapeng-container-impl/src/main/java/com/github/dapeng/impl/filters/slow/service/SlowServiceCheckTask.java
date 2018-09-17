package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;

import java.util.Optional;

public class SlowServiceCheckTask {

    protected final String serviceName;

    protected final String versionName;

    protected final String methodName;

    protected final long startTime;

    protected final Optional<Long> maxProcessTime;

    protected final Optional<Long> userId;

    protected final Optional<Integer> userIp;

    protected final Optional<Long> operatorId;

    protected final Optional<Integer> timeout;

    protected final Optional<Integer> calleeIp;

    protected final Optional<Integer> calleePort;

    protected final Optional<Long> callerTid;

    protected final Optional<String> callerMid;

    protected final Optional<Long> sessionTid;

    protected final int seqId;

    protected final Thread currentThread;

    public SlowServiceCheckTask(FilterContext ctx) {
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        this.startTime = System.currentTimeMillis();
        this.seqId = context.seqId();
        this.timeout = context.timeout();
        this.maxProcessTime = context.maxProcessTime();
        this.currentThread = Thread.currentThread();

        SoaHeader soaHeader = context.getHeader();
        this.serviceName = soaHeader.getServiceName();
        this.versionName = soaHeader.getVersionName();
        this.methodName = soaHeader.getMethodName();
        this.callerMid = soaHeader.getCallerMid();
        this.userId = soaHeader.getUserId();
        this.userIp = soaHeader.getCallerIp();
        this.operatorId = soaHeader.getOperatorId();
        this.calleeIp = soaHeader.getCalleeIp();
        this.calleePort = soaHeader.getCalleePort();
        this.callerTid = soaHeader.getCallerTid();
        this.sessionTid = soaHeader.getSessionTid();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(" [serviceName: ").append(serviceName).append(",")
                .append(" versionName: ").append(versionName).append(",")
                .append(" methodName: ").append(methodName).append(",")
                .append(" maxProcessTime: ").append(maxProcessTime).append(",")
                .append(" timeout: ").append(timeout).append(",")
                .append(" seqId: ").append(seqId).append(",")
                .append(" sessionTid: ").append(sessionTid.map(DapengUtil::longToHexStr).orElse("-")).append(",")
                .append(" startTime: ").append(startTime).append(",")
                .append(" userId: ").append(userId.orElse(0L)).append(",")
                .append(" userIp: ").append(userIp.map(IPUtils::transferIp).orElse("-")).append(",")
                .append(" operatorId: ").append(operatorId.orElse(0L)).append(",")
                .append(" calleeIp: ").append(calleeIp.map(IPUtils::transferIp).orElse("-")).append(",")
                .append(" calleePort: ").append(calleePort).append(",")
                .append(" callerTid: ").append(callerTid).append(",")
                .append(" callerMid: ").append(callerMid).append("]");

        sb.append(" \n");
        return sb.toString();
    }
}
