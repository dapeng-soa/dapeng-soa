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
 * TransactionContext 用于在服务提供方设置一些改变服务调用行为的属性.
 * 对应服务调用方的类为{@code InvocationContext}.
 * 服务端上下文
 *
 * @author craneding
 * @date 15/9/24
 */
public class TransactionContext {

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private SoaHeader header;

    private Integer seqid;

    private boolean isSoaGlobalTransactional;

    private Integer currentTransactionSequence = 0;

    private Integer currentTransactionId = 0;

    private SoaException soaException;

    private Optional<String> callerMid = Optional.empty();

    private Optional<String> callerIp = Optional.empty();

    private Optional<Integer> operatorId = Optional.empty();

    private Optional<Integer> userId = Optional.empty();




    public Optional<String> callerMid() {
        return callerMid;
    }

    public void callerMid(String callerMid) {
        this.callerMid = Optional.ofNullable(callerMid);
    }

    public Optional<String> callerIp() {
        return callerIp;
    }

    public void callerIp(Optional<String> callerIp) {
        this.callerIp = callerIp;
    }

    public Optional<Integer> operatorId() {
        return operatorId;
    }

    public void operatorId(Optional<Integer> operatorId) {
        this.operatorId = operatorId;
    }

    public Optional<String> operatorName() {
        return operatorName;
    }

    public void operatorName(Optional<String> operatorName) {
        this.operatorName = operatorName;
    }

    public Optional<Integer> customerId() {
        return userId;
    }

    public void customerId(Optional<Integer> customerId) {
        this.userId = customerId;
    }

    public Optional<String> customerName() {
        return customerName;
    }

    public void customerName(Optional<String> customerName) {
        this.customerName = customerName;
    }

    public void codecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public CodecProtocol codecProtocol() {
        return codecProtocol;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public void setHeader(SoaHeader header) {
        this.header = header;
    }

    public Integer getSeqid() {
        return seqid;
    }

    public void setSeqid(Integer seqid) {
        this.seqid = seqid;
    }

    public boolean isSoaGlobalTransactional() {
        return isSoaGlobalTransactional;
    }

    public void setSoaGlobalTransactional(boolean soaGlobalTransactional) {
        isSoaGlobalTransactional = soaGlobalTransactional;
    }

    public Integer currentTransactionSequence() {
        return currentTransactionSequence;
    }

    public void currentTransactionSequence(Integer currentTransactionSequence) {
        this.currentTransactionSequence = currentTransactionSequence;
    }

    public Integer currentTransactionId() {
        return currentTransactionId;
    }

    public void currentTransactionId(Integer currentTransactionId) {
        this.currentTransactionId = currentTransactionId;
    }

    public SoaException soaException() {
        return soaException;
    }

    public void soaException(SoaException soaException) {
        this.soaException = soaException;
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
