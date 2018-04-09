package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.core.enums.LoadBalanceStrategy;

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
 *
 * InvocationContext 用于在服务调用端设置一些改变服务调用行为的属性.
 * 对应服务提供方的类为{@code TransactionContext}.
 *
 * @author lihuimin
 * @date 2017/12/21
 */
public interface InvocationContext {
    /**
     * 设置服务会话ID
     *
     * @param sessionTid
     * @return
     */
    InvocationContext sessionTid(final String sessionTid);
    Optional<String> sessionTid();

    /**
     * 设置服务会话发起人Id, 特指前台用户,可用于频率控制
     *
     * @param userId
     * @return
     */
    InvocationContext userId(final Long userId);
    Optional<Long> userId();

    /**
     * 设置用户Ip,可用于频率控制
     *
     * @param userIp
     * @return
     */
    InvocationContext userIp(final String userIp);
    Optional<String> userIp();

    /**
     * 服务会话发起操作人Id, 特指后台用户
     * @param operatorId
     * @return
     */
    InvocationContext operatorId(final Long operatorId);
    Optional<Long> operatorId();
    /**
     * 设置超时,单位毫秒
     *
     * @param timeout
     * @return
     */
    InvocationContext timeout(final Integer timeout);
    Optional<Integer> timeout();

    /**
     * 设置thrift协议
     *
     * @param protocol
     * @return
     */
    InvocationContext codecProtocol(final CodecProtocol protocol);
    CodecProtocol codecProtocol();

    /**
     * 设置负载均衡策略
     *
     * @param loadBalanceStrategy
     * @return
     */
    InvocationContext loadBalanceStrategy(final LoadBalanceStrategy loadBalanceStrategy);
    Optional<LoadBalanceStrategy> loadBalanceStrategy();

    /**
     * 设置服务IP
     *
     * @param calleeIp
     * @return
     */
    InvocationContext calleeIp(final String calleeIp);
    Optional<String> calleeIp();

    /**
     * 设置服务端口
     *
     * @param calleePort
     * @return
     */
    InvocationContext calleePort(final Integer calleePort);
    Optional<Integer> calleePort();

    /**
     * 调用端tid
     * @return
     */
    InvocationContext callerTid(final String callerTid);
    String callerTid();

    /**
     * 设置调用端moudleId
     *
     * @param callerMid
     * @return
     */
    InvocationContext callerMid(final String callerMid);
    Optional<String> callerMid();

    /**
     * 供服务提供方返回时填写, 例如耗时, calleeIp等
     *
     * @param invocationInfo
     */
    InvocationContext lastInvocationInfo(InvocationInfo invocationInfo);
    InvocationInfo lastInvocationInfo();


    /**
     * 用于日志信息...
     * todo
     * @param seqId
     * @return
     */
    InvocationContext seqId(Integer seqId);
    Integer seqId();

    /**
     * 兼容目前的全局事务实现
     * @param currentTransactionId
     * @return
     */
    InvocationContext transactionId(Integer currentTransactionId);
    InvocationContext transactionSequence(Integer currentTransactionSequence);

    @Deprecated
    String serviceName();
    @Deprecated
    InvocationContext serviceName(String serviceName);

    @Deprecated
    String methodName();
    @Deprecated
    InvocationContext methodName(String methodName);

    @Deprecated
    String versionName();
    @Deprecated
    InvocationContext versionName(String versionName);

    interface InvocationInfo {
        /**
         * 服务提供方的tid
         *
         * @return
         */
        String calleeTid();

        /**
         * 服务IP
         *
         * @return
         */
        String calleeIp();

        /**
         * 服务端口
         *
         * @return
         */
        Integer calleePort();

        /**
         * 服务提供方MoudleId
         *
         * @return
         */
        String calleeMid();

        /**
         * 服务提供方消耗时间（从接收到请求 到 发送响应）,单位毫秒
         *
         * @return
         */
        Integer calleeTime1();

        /**
         * 服务提供方消耗时间（从开始处理请求到处理请求完成）,单位毫秒
         *
         * @return
         */
        Integer calleeTime2();

        /**
         * 从发起请求到收到响应所消耗的时间,单位毫秒
         *
         * @return
         */
        Integer serviceTime();
    }


    /*
        InvocationContext context = InvocationContextFactory.getInvocationContext();

        context.calleeIp("....");
        context.timeout(10s);

        someclient.somethod();

        context.getLastInfo().calleeIp();
        context.getLastInfo().getTid();

     */

}
