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

import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;

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
 * 2. 服务调用者: 单次服务调用的调用端,对应信息有caller的相关字段
 * 3. 服务发起者: 服务调用的最初发起者, 发起者也是调用者, 但是它调用的服务可能引发一连串的服务调用(也就是一次服务会话), 从而产生若干服务调用者. 一般是web层, 对应信息有userId,userIp
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
 * SoaHeader用于包装需传递到服务提供方的信息
 *
 * @author tangliu
 * @date 2016/1/11
 */
public class SoaHeader {

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

    /**
     * 服务会话ID, 在某次服务调用中会一直蔓延至本次服务调用引发的所有服务调用
     */
    private Optional<Long> sessionTid = Optional.empty();
    /**
     * 服务会话发起人Id, 特指前台用户
     */
    private Optional<Long> userId = Optional.empty();
    /**
     * 服务会话发起人Ip
     */
    private Optional<Integer> userIp = Optional.empty();
    /**
     * 服务会话发起操作人Id, 特指后台用户
     */
    private Optional<Long> operatorId = Optional.empty();


    private Optional<Long> callerTid = Optional.empty();

    private Optional<Integer> timeout = Optional.empty();

    /**
     * 慢服务检测时间阈值
     */
    private Optional<Long> maxProcessTime = Optional.empty();

    /**
     * 调用源
     */
    private Optional<String> callerMid = Optional.empty();

    /**
     * 调用者ip
     */
    private Optional<Integer> callerIp = Optional.empty();
    /**
     * 调用者port, 只有在dapeng容器内才需要设置这个值
     */
    private Optional<Integer> callerPort = Optional.empty();


    /**
     * 返回码
     */
    private Optional<String> respCode = Optional.empty();

    /**
     * 返回信息
     */
    private Optional<String> respMessage = Optional.empty();

    private Optional<Long> calleeTid = Optional.empty();
    private Optional<Integer> calleeIp = Optional.empty();
    private Optional<Integer> calleePort = Optional.empty();
    private Optional<String> calleeMid = Optional.empty();
    private Optional<Integer> calleeTime1 = Optional.empty();
    private Optional<Integer> calleeTime2 = Optional.empty();

    /**
     * 全局事务id
     */
    private Optional<Integer> transactionId = Optional.empty();

    /**
     * 当前过程所属序列号
     */
    private Optional<Integer> transactionSequence = Optional.empty();

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("{");

        sb.append("\"").append("serviceName").append("\":\"").append(this.serviceName).append("\",");
        sb.append("\"").append("methodName").append("\":\"").append(this.methodName).append("\",");
        sb.append("\"").append("versionName").append("\":\"").append(this.versionName).append("\",");
        sb.append("\"").append("sessionTid").append("\":\"").append(this.sessionTid.isPresent() ? DapengUtil.longToHexStr(this.sessionTid.get()) : null).append("\",");
        sb.append("\"").append("userId").append("\":\"").append(this.userId.isPresent() ? this.userId.get() : null).append("\",");
        sb.append("\"").append("userIp").append("\":\"").append(this.userIp.isPresent() ? IPUtils.transferIp(this.userIp.get()) : null).append("\",");
        sb.append("\"").append("operatorId").append("\":").append(this.operatorId.isPresent() ? this.operatorId.get() : null).append(",");
        sb.append("\"").append("timeout").append("\":\"").append(this.timeout.isPresent() ? this.timeout.get() : null).append("\",");
        sb.append("\"").append("maxProcessTime").append("\":\"").append(this.maxProcessTime.isPresent() ? this.maxProcessTime.get() : null).append("\",");
        sb.append("\"").append("callerTid").append("\":\"").append(this.callerTid.isPresent() ? DapengUtil.longToHexStr(this.callerTid.get()) : null).append("\",");
        sb.append("\"").append("callerMid").append("\":\"").append(this.callerMid.isPresent() ? this.callerMid.get() : null).append("\",");
        sb.append("\"").append("callerIp").append("\":\"").append(this.callerIp.isPresent() ? IPUtils.transferIp(this.callerIp.get()) : null).append("\",");
        sb.append("\"").append("callerPort").append("\":\"").append(this.callerPort).append("\",");
        sb.append("\"").append("calleeTid").append("\":\"").append(this.calleeTid.isPresent() ? DapengUtil.longToHexStr(this.calleeTid.get()) : null).append("\",");
        sb.append("\"").append("calleeMid").append("\":\"").append(this.calleeMid.isPresent() ? this.calleeMid.get() : null).append("\",");
        sb.append("\"").append("calleeIp").append("\":\"").append(this.calleeIp.isPresent() ? IPUtils.transferIp(this.calleeIp.get()) : null).append("\",");
        sb.append("\"").append("calleePort").append("\":\"").append(this.calleePort.isPresent() ? this.calleePort.get() : null).append("\",");
        sb.append("\"").append("calleeTime1").append("\":\"").append(this.calleeTime1.isPresent() ? this.calleeTime1.get() : null).append("\",");
        sb.append("\"").append("calleeTime2").append("\":\"").append(this.calleeTime2.isPresent() ? this.calleeTime2.get() : null).append("\",");
        sb.append("\"").append("transactionId").append("\":\"").append(this.transactionId.isPresent() ? this.transactionId.get() : null).append("\",");
        sb.append("\"").append("transactionSequence").append("\":\"").append(this.transactionSequence.isPresent() ? this.transactionSequence.get() : null).append("\",");
        sb.append("\"").append("cookies").append("\":\"").append(this.cookies).append("\",");
        sb.append("\"").append("respCode").append("\":\"").append(this.respCode.isPresent() ? this.respCode.get() : null).append("\",");
        sb.append("\"").append("respMessage").append("\":\"").append(this.respMessage.isPresent() ? this.respMessage.get() : null).append("\",");

        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append("}");
        return sb.toString();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public Optional<Long> getSessionTid() {
        return sessionTid;
    }

    public void setSessionTid(Optional<Long> sessionTid) {
        this.sessionTid = sessionTid;
    }

    public Optional<Long> getUserId() {
        return userId;
    }

    public void setUserId(Optional<Long> userId) {
        this.userId = userId;
    }

    public Optional<Integer> getUserIp() {
        return userIp;
    }

    public void setUserIp(Optional<Integer> userIp) {
        this.userIp = userIp;
    }

    public Optional<Long> getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Optional<Long> operatorId) {
        this.operatorId = operatorId;
    }

    public Optional<Long> getCallerTid() {
        return callerTid;
    }

    public void setCallerTid(Optional<Long> callerTid) {
        this.callerTid = callerTid;
    }

    public Optional<Integer> getTimeout() {
        return timeout;
    }

    public void setTimeout(Optional<Integer> timeout) {
        this.timeout = timeout;
    }

    public Optional<Long> getMaxProcessTime() {
        return maxProcessTime;
    }

    public void setMaxProcessTime(Optional<Long> maxProcessTime) {
        this.maxProcessTime = maxProcessTime;
    }

    public Optional<String> getCallerMid() {
        return callerMid;
    }

    public void setCallerMid(Optional<String> callerMid) {
        this.callerMid = callerMid;
    }

    public Optional<String> getRespCode() {
        return respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = Optional.ofNullable(respCode);
    }

    public Optional<String> getRespMessage() {
        return respMessage;
    }

    public void setRespMessage(String respMessage) {
        this.respMessage = Optional.ofNullable(respMessage);
    }

    public Optional<Long> getCalleeTid() {
        return calleeTid;
    }

    public void setCalleeTid(Long calleeTid) {
        this.calleeTid = Optional.ofNullable(calleeTid);
    }

    public Optional<Integer> getCalleeIp() {
        return calleeIp;
    }

    public void setCalleeIp(Optional<Integer> calleeIp) {
        this.calleeIp = calleeIp;
    }

    public Optional<Integer> getCalleePort() {
        return calleePort;
    }

    public void setCalleePort(Optional<Integer> calleePort) {
        this.calleePort = calleePort;
    }

    public Optional<String> getCalleeMid() {
        return calleeMid;
    }

    public void setCalleeMid(String calleeMid) {
        this.calleeMid = Optional.ofNullable(calleeMid);
    }

    public Optional<Integer> getCallerIp() {
        return this.callerIp;
    }

    public Optional<Integer> getCallerPort() {
        return this.callerPort;
    }

    public Optional<Integer> getCalleeTime1() {
        return calleeTime1;
    }

    public void setCalleeTime1(Integer calleeTime1) {
        this.calleeTime1 = Optional.ofNullable(calleeTime1);
    }

    public Optional<Integer> getCalleeTime2() {
        return calleeTime2;
    }

    public void setCalleeTime2(Integer calleeTime2) {
        this.calleeTime2 = Optional.ofNullable(calleeTime2);
    }

    public void setCallerIp(Integer callerIp) {
        this.callerIp = Optional.ofNullable(callerIp);
    }

    public void setCallerPort(Optional<Integer> callerPort) {
        this.callerPort = callerPort;
    }


    public Optional<Integer> getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = Optional.ofNullable(transactionId);
    }

    public Optional<Integer> getTransactionSequence() {
        return transactionSequence;
    }

    public void setTransactionSequence(Integer transactionSequence) {
        this.transactionSequence = Optional.ofNullable(transactionSequence);
    }

    /**
     * 添加备用字段，可以自定义传递消息
     */
    private Map<String, String> cookies;

    public void addCookie(String key, String value) {
        if (cookies == null) {
            cookies = new HashMap<>(16);
        }
        cookies.put(key, value);
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies == null ? new HashMap<>(16) : cookies;
    }

    public void addCookies(Map<String, String> cookies) {
        if (cookies == null) {
            return;
        }
        if (this.cookies == null) {
            this.cookies = new HashMap<>(16);
        }
        this.cookies.putAll(cookies);
    }

    public String getCookie(String key) {
        if (cookies == null) {
            return null;
        }
        return cookies.get(key);
    }

    public String getCookie(String key, String defaultValue) {
        if (cookies == null) {
            return defaultValue;
        }
        String value = cookies.get(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public Map<String, String> getCookies() {
        if (cookies == null) {
            cookies = new HashMap<>(16);
        }
        return cookies;
    }
}
