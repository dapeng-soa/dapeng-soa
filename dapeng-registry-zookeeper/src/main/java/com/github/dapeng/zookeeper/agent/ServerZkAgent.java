package com.github.dapeng.zookeeper.agent;

import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.zookeeper.client.ServerZk;

import java.util.Map;

/**
 * 服务端 zk agent
 *
 * @author huyj
 * @Created 2018/5/24 22:20
 */
public interface ServerZkAgent {


    /**
     * 启动  初始化
     */
    void init();

    /**
     * 停止
     */
    void stop();


    /**
     * 注册服务
     *
     * @param serverName  服务名
     * @param versionName 版本号
     */
    void registerService(String serverName, String versionName);


    /**
     * 注册服务集合
     */
    void registerAllServices();

    /**
     * 设置处理器集合
     *
     * @param processorMap 处理器集合
     */
    void setProcessorMap(Map<ProcessorKey, SoaServiceDefinition<?>> processorMap);

    /**
     * 获取处理器集合
     */
    //Map<ProcessorKey, SoaServiceDefinition<?>> getProcessorMap();


    /**
     * 获得 服务端代理 zk客户端
     *
     * @return
     */
    ServerZk getZkClient();

}
