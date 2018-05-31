package com.github.dapeng.zookeeper.agent;

import com.github.dapeng.zookeeper.common.ZkServiceInfo;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.zookeeper.client.ClientZk;

/**
 * 客户端 zkAgent
 *
 * @author huyj
 * @Created 2018/5/24 22:19
 */
public interface ClientZkAgent {

    /**
     * 启动  初始化
     */
    void init();

    /**
     * 停止
     */
    void stop();


    /**
     * 同步 服务状态信息
     *
     * @param zkServiceInfo
     */
    void syncService(ZkServiceInfo zkServiceInfo);


    /**
     * 注销服务
     *
     * @param zkServiceInfo
     */
    void cancelService(ZkServiceInfo zkServiceInfo);


    /**
     * 获得 客户端代理 zk客户端
     *
     * @return
     */
    ClientZk getZkClient();


    /**
     * 服务调用 在途统计 +1
     *
     * @param runtimeInstance
     */
    void activeCountIncrement(RuntimeInstance runtimeInstance);

    /**
     * 服务调用 在途统计 -1
     *
     * @param runtimeInstance
     */
    void activeCountDecrement(RuntimeInstance runtimeInstance);
}
