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

import java.util.HashMap;
import java.util.Map;
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
public class TransactionContextImpl implements TransactionContext {

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    /**
     * 服务会话ID, 在某次服务调用中会一直蔓延至本次服务调用引发的所有服务调用
     */
    private Optional<Long> sessionTid = Optional.empty();
    /**
     * 服务会话发起人Id, 特指前台用户
     */
    private Optional<Integer> customerId = Optional.empty();
    /**
     * 服务会话发起人Ip
     */
    private Optional<Integer> userIp = Optional.empty();
    /**
     * 服务会话发起操作人Id, 特指后台用户
     */
    private Optional<Integer> operatorId = Optional.empty();

    /**
     * 调用者Tid
     */
    private Optional<Long> callerTid = Optional.empty();
    /**
     * 调用者ip
     */
    private Optional<Integer> callerIp = Optional.empty();
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
     * 服务方法执行最大时间(慢服务)
     */
    private Optional<Long> maxProcessTime = Optional.empty();


    private Map<String, Object> attributes = new HashMap<>(16);


    /**
     * 用于服务调用传递. 当本服务作为调用者调用其它服务时, callerTid=calleeTid
     */
    private long calleeTid;


    private SoaHeader header;

    private int seqid;

    private SoaException soaException;

    /**
     * 全局事务相关信息
     */
    private boolean isSoaGlobalTransactional;

    private int currentTransactionSequence = 0;

    private int currentTransactionId = 0;


    @Override
    public Optional<String> callerMid() {
        return callerMid;
    }

    public TransactionContextImpl callerMid(String callerMid) {
        this.callerMid = Optional.ofNullable(callerMid);
        return this;
    }

    @Override
    public Optional<Integer> callerIp() {
        return callerIp;
    }

    public TransactionContextImpl callerIp(Integer callerIp) {
        this.callerIp = Optional.ofNullable(callerIp);
        return this;
    }

    @Override
    public Optional<Integer> operatorId() {
        return operatorId;
    }

    public TransactionContextImpl operatorId(Integer operatorId) {
        this.operatorId = Optional.ofNullable(operatorId);
        return this;
    }

    @Override
    public Optional<Integer> customerId() {
        return customerId;
    }

    public TransactionContextImpl customerId(Integer customerId) {
        this.customerId = Optional.ofNullable(customerId);
        return this;
    }

    @Override
    public TransactionContextImpl codecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
        return this;
    }

    @Override
    public CodecProtocol codecProtocol() {
        return codecProtocol;
    }

    @Override
    public SoaHeader getHeader() {
        return header;
    }

    public TransactionContextImpl setHeader(SoaHeader header) {
        this.header = header;
        return this;
    }

    @Override
    public int seqId() {
        return seqid;
    }

    public TransactionContextImpl setSeqid(int seqid) {
        this.seqid = seqid;
        return this;
    }

    @Override
    public boolean isSoaGlobalTransactional() {
        return isSoaGlobalTransactional;
    }

    @Override
    public TransactionContextImpl setSoaGlobalTransactional(boolean soaGlobalTransactional) {
        isSoaGlobalTransactional = soaGlobalTransactional;
        return this;
    }

    @Override
    public int currentTransactionSequence() {
        return currentTransactionSequence;
    }

    @Override
    public TransactionContextImpl currentTransactionSequence(int currentTransactionSequence) {
        this.currentTransactionSequence = currentTransactionSequence;
        return this;
    }

    @Override
    public int currentTransactionId() {
        return currentTransactionId;
    }

    @Override
    public TransactionContext currentTransactionId(int currentTransactionId) {
        this.currentTransactionId = currentTransactionId;
        return this;
    }

    @Override
    public SoaException soaException() {
        return soaException;
    }

    @Override
    public TransactionContextImpl soaException(SoaException soaException) {
        this.soaException = soaException;
        return this;
    }

    @Override
    public Optional<Integer> callerPort() {
        return callerPort;
    }

    public TransactionContextImpl callerPort(Integer callerPort) {
        this.callerPort = Optional.ofNullable(callerPort);
        return this;
    }

    @Override
    public Optional<Long> sessionTid() {
        return sessionTid;
    }

    public TransactionContextImpl sessionTid(Long sessionTid) {
        this.sessionTid = Optional.ofNullable(sessionTid);
        return this;
    }

    @Override
    public Optional<Integer> userIp() {
        return userIp;
    }

    public TransactionContextImpl userIp(Integer userIp) {
        this.userIp = Optional.ofNullable(userIp);
        return this;
    }

    @Override
    public Optional<Long> callerTid() {
        return callerTid;
    }

    public TransactionContextImpl callerTid(Long callerTid) {
        this.callerTid = Optional.ofNullable(callerTid);
        return this;
    }

    @Override
    public Optional<Integer> timeout() {
        return timeout;
    }

    public TransactionContextImpl timeout(Integer timeout) {
        this.timeout = Optional.ofNullable(timeout);
        return this;
    }

    @Override
    public Optional<Long> maxProcessTime() {
        return maxProcessTime;
    }

    @Override
    public TransactionContextImpl maxProcessTime(Long maxProcessTime) {
        this.maxProcessTime = Optional.ofNullable(maxProcessTime);
        return this;
    }

    @Override
    public long calleeTid() {
        return calleeTid;
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public TransactionContextImpl calleeTid(Long calleeTid) {
        this.calleeTid = calleeTid;
        return this;
    }
}
