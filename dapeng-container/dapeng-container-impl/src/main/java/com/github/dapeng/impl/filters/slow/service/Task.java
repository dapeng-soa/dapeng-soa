package com.github.dapeng.impl.filters.slow.service;

import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.filter.FilterContext;

import java.util.Optional;

public class Task {

    private String serviceName;

    private String versionName;

    private String methodName;

    private Object request;

    private long startTime;

    private Optional<Long> userId = Optional.empty();

    private Optional<String> userIp = Optional.empty();

    private Optional<Long> operatorId = Optional.empty();

    private Optional<Integer> timeout = Optional.empty();

    private Optional<String> calleeIp = Optional.empty();

    private Optional<Integer> calleePort = Optional.empty();

    private Optional<String> callerTid = Optional.empty();

    private Optional<String> callerMid = Optional.empty();

    private int seqId;

    private Thread currentThread;

    public Task(FilterContext ctx) {
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        this.startTime = System.currentTimeMillis();
        this.seqId = context.getSeqid();
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

        //设置request
        Object req = ctx.getAttribute("request");
        this.request = req;
    }

    public String serviceName() { return serviceName;}
    public Task serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }


    public String versionName() {return versionName;}
    public Task versionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public String methodName() {return methodName;}
    public Task methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }


    public long startTime() {return startTime;}
    public Task startTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public Optional<Long> userId() {return userId;}
    public Task userId(Optional<Long> userId) {
        this.userId = userId;
        return this;
    }

    public Optional<String> userIp() {return userIp; }
    public Task userIp(Optional<String> userIp) {
        this.userIp = userIp;
        return this;
    }

    public Optional<Long> operatorId() {return operatorId;}
    public Task operatorId(Optional<Long> operatorId) {
        this.operatorId = operatorId;
        return this;
    }

    public Optional<Integer> timeout() {return timeout;}
    public Task timeout(Optional<Integer> timeout) {
        this.timeout = timeout;
        return this;
    }

    public Optional<String> calleeIp() {return calleeIp;}
    public Task calleeIp(Optional<String> calleeIp) {
        this.calleeIp = calleeIp;
        return this;
    }

    public Optional<Integer> calleePort() {return calleePort;}
    public Task calleePort(Optional<Integer> calleePort) {
        this.calleePort = calleePort;
        return this;
    }

    public Optional<String> callerTid() {return this.callerTid;}
    public Task callerTid(Optional<String> callerTid) {
        this.callerTid = callerTid;
        return this;
    }

    public Optional<String> callerMid() {return callerMid;}
    public Task callerMid(Optional<String> callerMid) {
        this.callerMid = callerMid;
        return this;
    }

    public int seqId() {return seqId;}
    public Task seqId(int seqId) {
        this.seqId = seqId;
        return this;
    }

    public Thread currentThread() { return currentThread; }
    public Task currentThread(Thread currentThread) {
        this.currentThread = currentThread;
        return this;
    }

    public Object request() {return request;}
    public Object request(Object request) {
        this.request = request;
        return this;
    }


    /**
     *     private String serviceName;
     *     private String versionName;
     *     private String methodName;
     *     private long startTime;
     *     private Optional<Long> userId = Optional.empty();
     *     private Optional<String> userIp = Optional.empty();
     *     private Optional<Long> operatorId = Optional.empty();
     *     private Optional<Integer> timeout = Optional.empty();
     *     private Optional<String> calleeIp = Optional.empty();
     *     private Optional<Integer> calleePort = Optional.empty();
     *     private Optional<String> callerTid = Optional.empty();
     *     private Optional<String> callerMid = Optional.empty();
     *     private int seqId;
     * @return
     */
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

        sb.append(" \n request: ").append(request.toString());
        return sb.toString();
    }
}
