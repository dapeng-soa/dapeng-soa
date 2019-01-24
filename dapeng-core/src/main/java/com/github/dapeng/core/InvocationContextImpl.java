package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.dapeng.core.helper.DapengUtil.longToHexStr;
import static com.github.dapeng.core.helper.IPUtils.transferIp;

/**
 * 客户端上下文
 *
 * @author craneding
 * @date 15/9/24
 */

public class InvocationContextImpl implements InvocationContext {
    private final static int I_HOST_IP = IPUtils.transferIp(SoaSystemEnvProperties.HOST_IP);
    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 版本号
     */
    private String versionName;

    private Optional<Long> sessionTid = Optional.empty();
    private long callerTid = DapengUtil.generateTid();
    private Optional<Long> userId = Optional.empty();
    private Optional<Integer> userIp = Optional.empty();

    private Optional<Integer> timeout = Optional.empty();
    private Optional<Long> maxProcessTime = Optional.empty();

    private Optional<LoadBalanceStrategy> loadBalanceStrategy = Optional.empty();

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private Optional<Integer> calleeIp = Optional.empty();
    private Optional<Integer> calleePort = Optional.empty();

    private Optional<Integer> callerIp = Optional.ofNullable(I_HOST_IP);

    private Optional<String> callerMid = Optional.empty();

    private Optional<Long> operatorId = Optional.empty();

    private Optional<Integer> transactionId = Optional.empty();

    private Optional<Integer> transactionSequence = Optional.empty();

    private Map<String, String> cookies = new HashMap<>(16);

    /**
     * 包含服务提供端返回来的一些信息, 例如calleeIp, 服务耗时等信息
     */
    private InvocationInfo lastInvocationInfo;
    private int seqId;

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Deprecated
    public InvocationContext serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    public String methodName() {
        return methodName;
    }

    @Deprecated
    public InvocationContext methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    @Override
    public String versionName() {
        return versionName;
    }

    @Deprecated
    public InvocationContext seqId(Integer seqId) {
        this.seqId = seqId;
        return this;
    }

    @Override
    public int seqId() {
        return seqId;
    }

    @Deprecated
    public InvocationContext versionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    @Override
    public Optional<Integer> timeout() {
        return timeout;
    }

    @Override
    public InvocationContext timeout(final Integer timeout) {
        this.timeout = Optional.ofNullable(timeout);
        return this;
    }

    @Override
    public Optional<Long> maxProcessTime() {
        return maxProcessTime;
    }

    @Override
    public InvocationContext maxProcessTime(final Long maxProcessTime) {
        this.maxProcessTime = Optional.ofNullable(maxProcessTime);
        return this;
    }


    // read/write
    @Override
    public CodecProtocol codecProtocol() {
        return codecProtocol;
    }

    @Override
    public Optional<Integer> calleeIp() {
        return this.calleeIp;
    }

    @Override
    public InvocationContext calleeIp(Integer calleeIp) {
        this.calleeIp = Optional.ofNullable(calleeIp);
        return this;
    }

    @Override
    public Optional<Integer> calleePort() {
        return this.calleePort;
    }

    /**
     * should never call this method
     * @param callerIp
     * @return
     */
    @Override
    public InvocationContext callerIp(Integer callerIp) {
        this.callerIp = Optional.ofNullable(callerIp);
        return this;
    }

    @Override
    public Optional<Integer> callerIp() {
        return this.callerIp;
    }

    @Override
    public InvocationContext loadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = Optional.ofNullable(loadBalanceStrategy);
        return this;
    }

    @Override
    public Optional<LoadBalanceStrategy> loadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    @Override
    public InvocationContext calleePort(Integer calleePort) {
        this.calleePort = Optional.ofNullable(calleePort);
        return this;
    }

    @Override
    public InvocationInfo lastInvocationInfo() {
        return this.lastInvocationInfo;
    }

    @Override
    public InvocationContext transactionId(Integer currentTransactionId) {
        this.transactionId = Optional.ofNullable(currentTransactionId);
        return this;
    }

    @Override
    public InvocationContext transactionSequence(Integer currentTransactionSequence) {
        this.transactionSequence = Optional.ofNullable(currentTransactionSequence);
        return this;
    }

    @Override
    public InvocationContext sessionTid(Long sessionTid) {
        this.sessionTid = Optional.ofNullable(sessionTid);
        return this;
    }

    @Override
    public Optional<Long> sessionTid() {
        return this.sessionTid;
    }

    @Override
    public InvocationContext callerTid(Long callerTid) {
        this.callerTid = callerTid;
        return this;
    }

    @Override
    public long callerTid() {
        return this.callerTid;
    }

    public InvocationContext lastInvocationInfo(InvocationInfo lastInvocationInfo) {
        this.lastInvocationInfo = lastInvocationInfo;
        return this;
    }

    @Override
    public InvocationContext operatorId(Long operatorId) {
        this.operatorId = Optional.ofNullable(operatorId);
        return this;
    }

    @Override
    public Optional<Long> operatorId() {
        return this.operatorId;
    }


    @Override
    public InvocationContext callerMid(String callerMid) {
        this.callerMid = Optional.ofNullable(callerMid);
        return this;
    }

    @Override
    public Optional<String> callerMid() {
        return this.callerMid;
    }

    @Override
    public InvocationContext cookies(Map<String, String> cookies) {
        this.cookies.putAll(cookies);
        return this;
    }

    @Override
    public InvocationContext setCookie(String key, String value) {
        cookies.put(key, value);
        return this;
    }

    @Override
    public Map<String, String> cookies() {
        return Collections.unmodifiableMap(cookies);
    }

    @Override
    public String cookie(String key) {
        return cookies.get(key);
    }

    @Override
    public InvocationContext userId(Long userId) {
        this.userId = Optional.ofNullable(userId);
        return this;
    }

    @Override
    public Optional<Long> userId() {
        return userId;
    }

    @Override
    public InvocationContext userIp(Integer userIp) {
        this.userIp = Optional.ofNullable(userIp);
        return this;
    }

    @Override
    public Optional<Integer> userIp() {
        return userIp;
    }

    @Override
    public InvocationContext codecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"").append("serviceName").append("\":\"").append(this.serviceName).append("\",");
        sb.append("\"").append("methodName").append("\":\"").append(this.methodName).append("\",");
        sb.append("\"").append("versionName").append("\":\"").append(this.versionName).append("\",");
        sb.append("\"").append("sessionTid").append("\":\"").append(this.sessionTid.isPresent() ? longToHexStr(this.sessionTid.get()) : null).append("\",");
        sb.append("\"").append("userId").append("\":\"").append(this.userId.orElse(null)).append("\",");
        sb.append("\"").append("userIp").append("\":\"").append(this.userIp.isPresent() ? transferIp(this.userIp.get()) : null).append("\",");
        sb.append("\"").append("timeout").append("\":\"").append(this.timeout.orElse(null)).append("\",");
        sb.append("\"").append("maxProcessTime").append("\":\"").append(this.maxProcessTime.orElse(null)).append("\",");
        sb.append("\"").append("transactionId").append("\":\"").append(this.transactionId.orElse(null)).append("\",");
        sb.append("\"").append("transactionSequence").append("\":\"").append(this.transactionSequence.orElse(null)).append("\",");
        sb.append("\"").append("callerTid").append("\":\"").append(this.callerTid).append("\",");
        sb.append("\"").append("callerMid").append("\":\"").append(this.callerMid.orElse(null)).append("\",");
        sb.append("\"").append("operatorId").append("\":").append(this.operatorId.orElse(null)).append(",");
        sb.append("\"").append("calleeIp").append("\":\"").append(this.calleeIp.isPresent() ? transferIp(this.calleeIp.get()) : null).append("\",");
        sb.append("\"").append("calleePort").append("\":\"").append(this.calleePort.orElse(null)).append("\",");

        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("}");

        return sb.toString();
    }

    public static class Factory {
        private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<>();

        private static InvocationContextProxy invocationContextProxy;

        public static InvocationContextProxy getInvocationContextProxy() {
            return invocationContextProxy;
        }

        public static void setInvocationContextProxy(InvocationContextProxy invocationContextProxy) {
            Factory.invocationContextProxy = invocationContextProxy;
        }

        /**
         * must be invoked one time per thread before work begin
         *
         * @return
         */
        public static InvocationContext createNewInstance() {
            assert threadLocal.get() == null;

            InvocationContext context = new InvocationContextImpl();

            threadLocal.set(context);
            return context;
        }

        public static InvocationContext currentInstance() {
            InvocationContext context = threadLocal.get();

            //客户端可能不手动创建上下文.
            if (context == null) {
                context = createNewInstance();
            }

            //容器内调用其他服务的时候，需要在路由之前把调用信息赋值给invocationContext
            if (TransactionContext.hasCurrentInstance()) {
                TransactionContext tc = TransactionContext.Factory.currentInstance();
                SoaHeader oriHeader = tc.getHeader();
                if (oriHeader != null) {
                    if (oriHeader.getOperatorId().isPresent()) {
                        context.operatorId(oriHeader.getOperatorId().get());
                    }
                    if (oriHeader.getUserId().isPresent()) {
                        context.userId(oriHeader.getUserId().get());
                    }
                    if (oriHeader.getUserIp().isPresent()) {
                        context.userIp(oriHeader.getUserIp().get());
                    }
                    context.cookies(oriHeader.getCookies());
                }
            }

            return context;
        }

        public static InvocationContext currentInstance(InvocationContext context) {
            threadLocal.set(context);

            return context;
        }

        /**
         * must be invoked after work done
         *
         * @return
         */
        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }


}
