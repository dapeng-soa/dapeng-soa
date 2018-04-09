package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * <pre>
 * web	service1	service2	service3	service4
 *  |_____m1()
 *  |__________________m2()
 *  |                  |_______________________m4()
 *  |_______________________________m3()
 *
 * 1. 服务session: 如上图是三个服务会话,由服务发起者(web)三次服务调用引发的一系列服务调用:
 *  1.1 web->m1
 *  1.2 web->m2->m4
 *  1.3 web->m3
 * 2. 服务调用者: 单次服务调用的调用端(例如对于1.2的服务会话中, web以及service2都是服务调用端),对应信息有caller的相关字段
 * 3. 服务发起者: 服务调用的最初发起者, 发起者也是调用者, 但是它调用的服务可能引发一连串的服务调用(也就是一次服务会话), 从而产生若干服务调用者. 上图三个服务会话的发起者都是web层, 对应信息有userId,userIp
 * 4. caller信息:
 *   4.1 callerMid: 服务全限定名, serviceName:method:version, web->URL, script, task
 *   4.2 callerIp: ip
 *   4.3 callerPort:
 *   4.4 callerTid: 服务调用者的tid,一般指服务实现者的tid.
 *   4.5 serviceTime: 服务调用耗时(从发出请求到收到响应,包括网络开销)
 * 5. sessionTid: 由服务发起者创建的全局唯一id, 通过InvocationContext传递,用于跟踪一次完整的服务调用过程. 当SessionTid为0时，服务实现端会使用当前创建的Tid作为sessionTid
 * 6. callee信息, 通过InvocationContext的lastInfo返回:
 *   6.1 calleeId: 服务全限定名, serviceName:method:version, web->URL, script, task
 *   6.2 calleeUri: ip:port
 *   6.3 calleeTid: 服务被调者的tid
 *   6.4 calleeTime1,//服务提供方消耗时间（从接收到请求 到 发送响应）,单位毫秒
 *   6.5 calleeTime2,//服务提供方消耗时间（从开始处理请求到处理请求完成）,单位毫秒
 * </pre>
 * <p>
 * TransactionContext 用于在服务提供方保存一些服务调用信息.
 * 对应服务调用方的类为{@code InvocationContext}.
 * 服务端上下文
 *
 * @author craneding
 * @date 15/9/24
 */
public class TransactionContext {

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    /**
     * 服务会话ID, 在某次服务调用中会一直蔓延至本次服务调用引发的所有服务调用
     */
    private Optional<String> sessionTid = Optional.empty();
    /**
     * 服务会话发起人Id, 特指前台用户
     */
    private Optional<Long> userId = Optional.empty();
    /**
     * 服务会话发起人Ip
     */
    private Optional<String> userIp = Optional.empty();
    /**
     * 服务会话发起操作人Id, 特指后台用户
     */
    private Optional<Long> operatorId = Optional.empty();

    /**
     * 调用者Tid
     */
    private Optional<String> callerTid = Optional.empty();
    /**
     * 调用者ip
     */
    private String callerIp;
    /**
     * 调用者port, 只有dapeng服务作为调用者的时候才有这个值
     */
    private Optional<Integer> callerPort = Optional.empty();

    /**
     * 客户端指定的超时
     */
    private Optional<Integer> timeout = Optional.empty();
    /**
     * 调用源
     */
    private Optional<String> callerMid = Optional.empty();


    /**
     * 用于服务调用传递. 当本服务作为调用者调用其它服务时, callerTid=calleeTid
     */
    private String calleeTid;


    private SoaHeader header;

    private Integer seqid;

    private SoaException soaException;

    /**
     * 全局事务相关信息
     */
    private boolean isSoaGlobalTransactional;

    private Integer currentTransactionSequence = 0;

    private Integer currentTransactionId = 0;


    public Optional<String> callerMid() {
        return callerMid;
    }

    public TransactionContext callerMid(String callerMid) {
        this.callerMid = Optional.ofNullable(callerMid);
        return this;
    }

    public String callerIp() {
        return callerIp;
    }

    public TransactionContext callerIp(String callerIp) {
        this.callerIp = callerIp;
        return this;
    }

    public Optional<Long> operatorId() {
        return operatorId;
    }

    public TransactionContext operatorId(Long operatorId) {
        this.operatorId = Optional.ofNullable(operatorId);
        return this;
    }

    public Optional<Long> userId() {
        return userId;
    }

    public TransactionContext userId(Long userId) {
        this.userId = Optional.ofNullable(userId);
        return this;
    }

    public TransactionContext codecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
        return this;
    }

    public CodecProtocol codecProtocol() {
        return codecProtocol;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public TransactionContext setHeader(SoaHeader header) {
        this.header = header;
        return  this;
    }

    public Integer getSeqid() {
        return seqid;
    }

    public TransactionContext setSeqid(Integer seqid) {
        this.seqid = seqid;
        return this;
    }

    public boolean isSoaGlobalTransactional() {
        return isSoaGlobalTransactional;
    }

    public TransactionContext setSoaGlobalTransactional(boolean soaGlobalTransactional) {
        isSoaGlobalTransactional = soaGlobalTransactional;
        return this;
    }

    public Integer currentTransactionSequence() {
        return currentTransactionSequence;
    }

    public TransactionContext currentTransactionSequence(Integer currentTransactionSequence) {
        this.currentTransactionSequence = currentTransactionSequence;
        return this;
    }

    public Integer currentTransactionId() {
        return currentTransactionId;
    }

    public TransactionContext currentTransactionId(Integer currentTransactionId) {
        this.currentTransactionId = currentTransactionId;
        return this;
    }

    public SoaException soaException() {
        return soaException;
    }

    public TransactionContext soaException(SoaException soaException) {
        this.soaException = soaException;
        return this;
    }

    public Optional<Integer> callerPort() {
        return callerPort;
    }

    public TransactionContext callerPort(Integer callerPort) {
        this.callerPort = Optional.ofNullable(callerPort);
        return this;
    }

    public Optional<String> sessionTid() {
        return sessionTid;
    }

    public TransactionContext sessionTid(String sessionTid) {
        this.sessionTid = Optional.ofNullable(sessionTid);
        return this;
    }

    public Optional<String> userIp() {
        return userIp;
    }

    public TransactionContext userIp(String userIp) {
        this.userIp = Optional.ofNullable(userIp);
        return this;
    }

    public Optional<String> callerTid() {
        return callerTid;
    }

    public TransactionContext callerTid(String callerTid) {
        this.callerTid = Optional.ofNullable(callerTid);
        return this;
    }

    public Optional<Integer> timeout() {
        return timeout;
    }

    public TransactionContext timeout(Integer timeout) {
        this.timeout = Optional.ofNullable(timeout);
        return this;
    }

    public String calleeTid() {
        return calleeTid;
    }

    public TransactionContext calleeTid(String calleeTid) {
        this.calleeTid = calleeTid;
        return this;
    }

    public static class Factory {
        private static ThreadLocal<TransactionContext> threadLocal = new ThreadLocal<>();

        /**
         * 确保在业务线程入口设置context
         *
         * @return
         */
        public static TransactionContext createNewInstance() {
            assert (threadLocal.get() == null);

            TransactionContext context = new TransactionContext();
            threadLocal.set(context);
            return context;
        }

        public static TransactionContext currentInstance(TransactionContext context) {
            threadLocal.set(context);

            return context;
        }

        public static TransactionContext currentInstance() {
            TransactionContext context = threadLocal.get();

            if (context == null) {
                context = createNewInstance();
            }

            return context;
        }

        /**
         * 确保在业务线程出口清除context
         */
        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }

    /**
     * call by client checking whether thread is in container
     *
     * @return
     */
    public static boolean hasCurrentInstance() {
        return Factory.threadLocal.get() != null;
    }
}
