/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * <pre>
 * web	service1	service2	service3	service4
 *  |_____m1()
 *         |___________m2()
 *         |            |______________________m4()
 *         |_______________________m3()
 *
 * 1. 服务session: 如上图是一个服务会话,由服务发起者(web)一次服务调用引发的一系列服务调用
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
public interface TransactionContext {

    Optional<String> callerMid();

    Optional<Integer> callerIp();

    Optional<Integer> operatorId();

    Optional<Integer> customerId();

    TransactionContext codecProtocol(CodecProtocol codecProtocol);

    CodecProtocol codecProtocol();

    SoaHeader getHeader();

    int seqId();

    boolean isSoaGlobalTransactional();

    TransactionContext setSoaGlobalTransactional(boolean soaGlobalTransactional);

    int currentTransactionSequence();

    TransactionContext currentTransactionSequence(int currentTransactionSequence);

    int currentTransactionId();

    TransactionContext currentTransactionId(int currentTransactionId);

    SoaException soaException();

    TransactionContext soaException(SoaException soaException);

    Optional<Integer> callerPort();

    Optional<Long> sessionTid();

    Optional<Integer> userIp();

    Optional<Long> callerTid();

    Optional<Integer> timeout();

    Optional<Long> maxProcessTime();
    TransactionContext maxProcessTime(Long maxProcessTime);

    long calleeTid();

    void setAttribute(String key, Object value);
    Object getAttribute(String key);

    TransactionContext calleeTid(Long calleeTid);

    class Factory {
        private static ThreadLocal<TransactionContext> threadLocal = new ThreadLocal<>();

        /**
         * 确保在业务线程入口设置context
         *
         * @return
         */
        public static TransactionContext createNewInstance() {
            assert (threadLocal.get() == null);

            TransactionContext context = new TransactionContextImpl();
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
    static boolean hasCurrentInstance() {
        return TransactionContext.Factory.threadLocal.get() != null;
    }
}
