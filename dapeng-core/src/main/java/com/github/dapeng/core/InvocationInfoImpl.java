package com.github.dapeng.core;

import com.github.dapeng.core.enums.LoadBalanceStrategy;

/**
 * <pre>
 * web	service1	service2	service3	service4
 *  |_____m1()
 *         |___________m2()
 *         |           |_______________________m4()
 *         |_______________________m3()
 *
 * 1. 服务session: 如上图是一次完整的服务会话过程,由服务发起者(web)一次服务调用引发的一系列服务调用
 * 2. 服务调用者: 单次服务调用的调用端,对应信息有caller的相关字段
 * 3. 服务发起者: 服务调用的最初发起者, 发起者也是调用者, 但是它调用的服务可能引发一连串的服务调用(也就是一次服务会话), 从而产生若干服务调用者. 一般是web层, 对应信息有userId,userIp
 * 4. caller信息:
 *   4.1 callerMid: 服务全限定名, serviceName:method:version, web->URL, script, task
 *   4.2 callerIp: ip
 *   4.3 callerPort:
 *   4.4 callerTid: 服务调用者的tid,一般指服务实现者的tid.
 *   4.5 serviceTime: 服务调用耗时(从发出请求到收到响应,包括网络开销)
 * 5. sessionTid: 由服务发起者创建的全局唯一id, 通过InvocationContext传递,用于跟踪一次完整的服务调用过程. 当SessionTid为0时，服务实现端会使用当前创建的Tid作为sessionTid
 * 6. callee信息, 通过InvocationContext的lastInvocationInfo返回:
 *   6.1 calleeId: 服务全限定名, serviceName:method:version, web->URL, script, task
 *   6.2 calleeUri: ip:port
 *   6.3 calleeTid: 服务被调者的tid
 *   6.4 calleeTime1,//服务提供方消耗时间（从接收到请求 到 发送响应）,单位毫秒
 *   6.5 calleeTime2,//服务提供方消耗时间（从开始处理请求到处理请求完成）,单位毫秒
 * </pre>
 *
 * InvocationInfoImpl用于包装服务提供方返回的信息
 * @author lihuimin
 * @date 2017/12/22
 */
public class InvocationInfoImpl implements InvocationContext.InvocationInfo {
    private  String calleeTid;
    private  String calleeIp;
    private  int calleePort;
    private  String calleeMid;
    private  int calleeTime1;
    private  int calleeTime2;
    private long serviceTime;
    private  LoadBalanceStrategy loadBalanceStrategy;

    public InvocationInfoImpl(String calleeTid, String calleeIp,
                              int calleePort, String calleeMid,
                              int calleeTime1, int calleeTime2,
                              long serviceTime, LoadBalanceStrategy loadBalanceStrategy) {
        this.calleeTid = calleeTid;
        this.calleeIp = calleeIp;
        this.calleePort = calleePort;
        this.calleeMid = calleeMid;
        this.calleeTime1 = calleeTime1;
        this.calleeTime2 = calleeTime2;
        this.serviceTime = serviceTime;
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    public void calleeTid(String calleeTid) {
        this.calleeTid = calleeTid;
    }

    public void calleeIp(String calleeIp) {
        this.calleeIp = calleeIp;
    }

    public void calleePort(int calleePort) {
        this.calleePort = calleePort;
    }

    public void calleeMid(String calleeMid) {
        this.calleeMid = calleeMid;
    }

    public void calleeTime1(int calleeTime1) {
        this.calleeTime1 = calleeTime1;
    }

    public void calleeTime2(int calleeTime2) {
        this.calleeTime2 = calleeTime2;
    }

    public void loadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    public InvocationInfoImpl(){

    }

    @Override
    public String calleeTid() {
        return calleeTid;
    }

    @Override
    public String calleeIp() {
        return calleeIp;
    }

    @Override
    public int calleePort() {
        return calleePort;
    }

    @Override
    public String calleeMid() {
        return calleeMid;
    }

    @Override
    public int calleeTime1() {
        return calleeTime1;
    }

    @Override
    public int calleeTime2() {
        return calleeTime2;
    }

    @Override
    public long serviceTime() {
        return serviceTime;
    }

    @Override
    public LoadBalanceStrategy loadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    public void serviceTime(long serviceTime){
        this.serviceTime=serviceTime;
    }

    @Override
    public String toString() {
        return String.format(
                "calleeIp->%s,calleeMid->%s,calleePort->%s,calleeTid->%s,calleeTime1->%s,calleeTime2->%s,loadBalanceStrategy->%s,serviceTime->%s",
                this.calleeIp,
                this.calleeMid,
                this.calleePort,
                this.calleeTid,
                this.calleeTime1,
                this.calleeTime2,
                this.loadBalanceStrategy,
                this.serviceTime);
    }
}
