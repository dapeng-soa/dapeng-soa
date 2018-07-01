package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.FilterContext;

import java.util.Optional;

public class SlowServiceCheckTask {

    private String serviceName;

    private String versionName;

    private String methodName;

    private Object request;

    private long startTime;

    private Optional<Long> userId = Optional.empty();

    private Optional<Integer> userIp = Optional.empty();

    private Optional<Long> operatorId = Optional.empty();

    private Optional<Integer> timeout = Optional.empty();

    private Optional<Integer> calleeIp = Optional.empty();

    private Optional<Integer> calleePort = Optional.empty();

    private Optional<Long> callerTid = Optional.empty();

    private Optional<String> callerMid = Optional.empty();

    private int seqId;

    private Thread currentThread;

    public SlowServiceCheckTask(FilterContext ctx) {
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        this.startTime = System.currentTimeMillis();
        this.seqId = context.seqId();
        this.timeout = context.timeout();
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
        this.callerMid = soaHeader.getCallerMid();

    }

    public String serviceName() {
        return serviceName;
    }

    public SlowServiceCheckTask serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }


    public String versionName() {
        return versionName;
    }

    public SlowServiceCheckTask versionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public String methodName() {
        return methodName;
    }

    public SlowServiceCheckTask methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }


    public long startTime() {
        return startTime;
    }

    public SlowServiceCheckTask startTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public Optional<Long> userId() {
        return userId;
    }

    public SlowServiceCheckTask userId(Optional<Long> userId) {
        this.userId = userId;
        return this;
    }

    public Optional<Integer> userIp() {
        return userIp;
    }

    public SlowServiceCheckTask userIp(Optional<Integer> userIp) {
        this.userIp = userIp;
        return this;
    }

    public Optional<Long> operatorId() {
        return operatorId;
    }

    public SlowServiceCheckTask operatorId(Optional<Long> operatorId) {
        this.operatorId = operatorId;
        return this;
    }

    public Optional<Integer> timeout() {
        return timeout;
    }

    public SlowServiceCheckTask timeout(Optional<Integer> timeout) {
        this.timeout = timeout;
        return this;
    }

    public Optional<Integer> calleeIp() {
        return calleeIp;
    }

    public SlowServiceCheckTask calleeIp(Optional<Integer> calleeIp) {
        this.calleeIp = calleeIp;
        return this;
    }

    public Optional<Integer> calleePort() {
        return calleePort;
    }

    public SlowServiceCheckTask calleePort(Optional<Integer> calleePort) {
        this.calleePort = calleePort;
        return this;
    }

    public Optional<Long> callerTid() {
        return this.callerTid;
    }

    public SlowServiceCheckTask callerTid(Optional<Long> callerTid) {
        this.callerTid = callerTid;
        return this;
    }

    public Optional<String> callerMid() {
        return callerMid;
    }

    public SlowServiceCheckTask callerMid(Optional<String> callerMid) {
        this.callerMid = callerMid;
        return this;
    }

    public int seqId() {
        return seqId;
    }

    public SlowServiceCheckTask seqId(int seqId) {
        this.seqId = seqId;
        return this;
    }

    public Thread currentThread() {
        return currentThread;
    }

    public SlowServiceCheckTask currentThread(Thread currentThread) {
        this.currentThread = currentThread;
        return this;
    }

    public Object request() {
        return request;
    }

    public Object request(Object request) {
        this.request = request;
        return this;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(" [serviceName: ").append(serviceName).append(",")
                .append(" versionName: ").append(versionName).append(",")
                .append(" methodName: ").append(methodName).append(",")
                .append(" seqId: ").append(seqId).append(",")
                .append(" startTime: ").append(startTime).append(",")
                .append(" userId: ").append(userId).append(",")
                .append(" userIp: ").append(userIp).append(",")
                .append(" operatorId: ").append(operatorId).append(",")
                .append(" timeout: ").append(timeout).append(",")
                .append(" calleeIp: ").append(calleeIp).append(",")
                .append(" calleePort: ").append(calleePort).append(",")
                .append(" callerTid: ").append(callerTid).append(",")
                .append(" callerMid: ").append(callerMid).append("]");

        sb.append(" \n");
        return sb.toString();
    }
}
