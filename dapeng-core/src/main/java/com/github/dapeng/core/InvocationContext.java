package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * todo
 * 对上层应用暴露的接口, 非必要不需要提供setter方法
 * @author lihuimin
 * @date 2017/12/21
 */
public interface InvocationContext {

    @Deprecated
    void setServiceName(String serviceName);

    String getServiceName();

    @Deprecated
    void setMethodName(String methodName);

    String getMethodName();

    void setVersionName(String versionName);

    String getVersionName();

    void setCodecProtocol(CodecProtocol protocol);

    CodecProtocol getCodecProtocol();

    Optional<String> getCalleeIp();

    void setCalleeIp(Optional<String> calleeIp);

    Optional<Integer> getCalleePort();

    void setCalleePort(Optional<Integer> calleePort);

    Optional<Integer> getTransactionId();

    @Deprecated
    void setTransactionId(Optional<Integer> transactionId);

    void setCustomerId(Optional<Integer> customerId);

    Optional<Integer> getCustomerId();

    void setCustomerName(Optional<String> customerName);

    Optional<String> getCustomerName();

    void setOperatorId(Optional<Integer> operatorId);

    Optional<Integer> getOperatorId();

    void setOperatorName(Optional<String> operatorName);

    Optional<String> getOperatorName();

    void setCallerFrom(Optional<String> callerFrom);

    Optional<String> getCallerFrom();

    void setCallerIp(Optional<String> callerIp);

    Optional<String> getCallerIp();

    @Deprecated
    void setTransactionSequence(Optional<Integer>transactionSequence);

    /**
     * 共callee返回时填写, 例如耗时, calleeIp等
     * @param invocationInfo
     */
    void setLastInfo(InvocationInfo invocationInfo);

    InvocationInfo getLastInfo();

    void setSessionId(Optional<String>  sessionId);

    Optional<String> getSessionId();

    boolean isSoaTransactionProcess();

    void setSoaTransactionProcess(Boolean isSoaTransactionProcess);

    public Optional<Long> getTimeout();

    public void setTimeout(Optional<Long> timeout);

    // seqid
    // tid
    interface InvocationInfo {

    }

//    interface Set {
//        // codecProtocol
//        // calleeIp, calleePort
//        // loadbalance
//        // timeout
//        // sessionid
//        // cookie
//        // uid
//        // staffid
//    }




    /*
        InvocationContext context = InvocationContextFactory.getInvocationContext();

        context.setCalleeIp("....");
        context.setTimeout(10s);

        someclient.somethod();

        context.getLastInfo().getCalleeIp();
        context.getLastInfo().getTid();

     */

}
