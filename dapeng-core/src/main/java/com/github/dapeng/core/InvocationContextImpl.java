package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * 客户端上下文
 *
 * @author craneding
 * @date 15/9/24
 */

public class InvocationContextImpl implements  InvocationContext {

    private String serviceName;

    private String methodName;

    private String versionName;

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private Optional<String> calleeIp;

    private Optional<Integer> calleePort;

    private Optional<String> callerFrom = Optional.empty();

    private Optional<String> callerIp = Optional.empty();

    private Optional<Integer> operatorId = Optional.empty();

    private Optional<String> operatorName = Optional.empty();

    private Optional<Integer> customerId = Optional.empty();

    private Optional<String> customerName = Optional.empty();

    private Optional<Integer> transactionSequence = Optional.empty();

    private InvocationInfo invocationInfo;

    private Optional<String> sessionId = Optional.empty();

    /**
     * 全局事务id
     */
    private Optional<Integer> transactionId = Optional.empty();

    // readonly
    private int seqid;

    private boolean isSoaTransactionProcess;

    // read/write
    @Override
    public CodecProtocol getCodecProtocol() {
        return codecProtocol;
    }

    @Override
    public Optional<String> getCalleeIp() {
        return this.calleeIp;
    }

    @Override
    public void setCalleeIp(Optional<String> calleeIp) {
        this.calleeIp = calleeIp;
    }

    @Override
    public Optional<Integer> getCalleePort() {
        return this.calleePort;
    }

    @Override
    public void setCalleePort(Optional<Integer> calleePort) {
        this.calleePort = calleePort;
    }

    @Override
    public InvocationInfo getLastInfo() {
        return this.invocationInfo;
    }

    @Override
    public void setSessionId(Optional<String> sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public Optional<String> getSessionId() {
        return this.sessionId;
    }

    @Override
    public boolean isSoaTransactionProcess() {
      return this.isSoaTransactionProcess;
    }

    @Override
    public void setSoaTransactionProcess(Boolean isSoaTransactionProcess) {
        this.isSoaTransactionProcess = isSoaTransactionProcess;
    }

    @Override
    public void setLastInfo(InvocationInfo invocationInfo) {
        this.invocationInfo = invocationInfo;
    }

    @Override
    public Optional<Integer> getTransactionId() {
        return transactionId;
    }

    @Override
    public void setTransactionId(Optional<Integer> transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public void setCustomerId(Optional<Integer> customerId) {

    }

    @Override
    public Optional<Integer> getCustomerId() {
        return this.customerId;
    }

    @Override
    public void setCustomerName(Optional<String> customerName) {
        this.customerName = customerName;
    }

    @Override
    public Optional<String> getCustomerName() {
        return this.customerName;
    }

    @Override
    public void setOperatorId(Optional<Integer> operatorId) {
        this.operatorId = operatorId;
    }

    @Override
    public Optional<Integer> getOperatorId() {
        return this.operatorId;
    }

    @Override
    public void setOperatorName(Optional<String> operatorName) {
        this.operatorName = operatorName;
    }

    @Override
    public Optional<String> getOperatorName() {
        return this.operatorName;
    }

    @Override
    public void setCallerFrom(Optional<String> callerFrom) {
        this.callerFrom = callerFrom;
    }

    @Override
    public Optional<String> getCallerFrom() {
        return this.callerFrom;
    }

    @Override
    public void setCallerIp(Optional<String> callerIp) {
        this.callerIp = callerIp;
    }

    @Override
    public Optional<String> getCallerIp() {
        return this.callerIp;
    }

    @Override
    public void setTransactionSequence(Optional<Integer> transactionSequence) {
        this.transactionSequence = transactionSequence;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }

    @Override
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Override
    public String getVersionName() {
        return this.versionName;
    }

    @Override
    public void setCodecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public int getSeqid() {
        return seqid;
    }

    public void setSeqid(int seqid) {
        this.seqid = seqid;
    }

    public static class Factory {
        private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<>();

        private static InvocationContextProxy invocationContextProxy;

        public static interface InvocationContextProxy {

            Optional<String> callerFrom();

            Optional<Integer> customerId();

            Optional<String> customerName();

            Optional<Integer> operatorId();

            Optional<String> operatorName();

            Optional<String> sessionId();
        }

        public static void setInvocationContextProxy(InvocationContextProxy invocationContextProxy) {
            Factory.invocationContextProxy = invocationContextProxy;
        }

        public static InvocationContextProxy getInvocationContextProxy() {
            return invocationContextProxy;
        }
        /**
         * must be invoked one time per thread before work begin
         * @return
         */
        public static InvocationContext createNewInstance() {
            assert threadLocal.get() == null;

            InvocationContext context = new InvocationContextImpl();
            threadLocal.set(context);
            return context;
        }

        public static InvocationContext getCurrentInstance() {
            InvocationContext context = threadLocal.get();

            //客户端可能不手动创建上下文.
            if (context == null) {
                context = createNewInstance();

                threadLocal.set(context);
            }

            return context;
        }

        /**
         * must be invoked after work done
         * @return
         */
        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }


}
