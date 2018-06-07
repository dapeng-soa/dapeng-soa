package com.github.dapeng.zookeeper.agent.impl;

import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.zookeeper.agent.ServerZkAgent;
import com.github.dapeng.zookeeper.client.ServerZk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.dapeng.zookeeper.common.BaseConfig.*;

/**
 * 服务端 zk agent 实现
 *
 * @author huyj
 * @Created 2018/5/24 22:22
 */
public class ServerZkAgentImpl implements ServerZkAgent {
    private static final Logger logger = LoggerFactory.getLogger(ServerZkAgentImpl.class);
    private static final ServerZkAgentImpl serverZkAgentInstance = new ServerZkAgentImpl();

    /**
     * 是否使用 灰度 zk
     */
    private final boolean usingFallbackZk = SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_ISCONFIG;
    private static ServerZk masterZk, fallbackZk;
    private Map<ProcessorKey, SoaServiceDefinition<?>> processorMap;

    private ServerZkAgentImpl() {
        init();
    }

    public static ServerZkAgent getServerZkAgentInstance() {
        return serverZkAgentInstance;
    }

    @Override
    public void init() {
        masterZk = new ServerZk(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        if (usingFallbackZk) {
            fallbackZk = new ServerZk(SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_HOST);
        }
    }

    @Override
    public void stop() {
        if (masterZk != null) {
            masterZk.destroy();
        }
        if (fallbackZk != null) {
            fallbackZk.destroy();
        }
    }

    @Override
    public ServerZk getZkClient() {
        return masterZk;
    }

    @Override
    public void registerService(String serverName, String versionName) {
        try {
            String instanceInfo = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
            String path = RUNTIME_PATH + "/" + serverName + "/" + instanceInfo;
            //String servicePath = RUNTIME_PATH + "/" + serverName;

            // 注册服务 runtime 实例 到 zk  临时节点
            if (usingFallbackZk) {
                fallbackZk.createEphemeralSequential(path, null);
            } else {
                masterZk.createEphemeralSequential(path, null);
            }

            createPreparePath(serverName);
        } catch (Exception e) {
            logger.error("registerService failed..");
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void registerAllServices() {
        List<Service> services = getAllServices();
        if (services == null) {
            return;
        }
        services.forEach(service -> registerService(service.name(), service.version()));
        //如果开启了全局事务，将事务服务也注册到zookeeper,为了主从竞选，只有主全局事务管理器会执行
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {
            this.registerService("com.github.dapeng.transaction.api.service.GlobalTransactionService", "1.0.0");
        }
    }

    public Map<ProcessorKey, SoaServiceDefinition<?>> getProcessorMap() {
        return processorMap;
    }

    public void setProcessorMap(Map<ProcessorKey, SoaServiceDefinition<?>> processorMap) {
        this.processorMap = processorMap;
    }

    /**
     * 在 registerService 到zk 时，注册需要准备的各节点
     *
     * @param serverName 服务名
     */
    private void createPreparePath(String serverName) {
        try {
            if (usingFallbackZk) {
                // 创建  zk  runtime imstances
                fallbackZk.createPersistent(RUNTIME_PATH + "/" + serverName, null);
                // 创建  zk  config 服务 持久节点 \
                fallbackZk.createPersistent(CONFIG_PATH + "/" + serverName, null);
                // 创建路由节点
                fallbackZk.createPersistent(ROUTES_PATH + "/" + serverName, null);
                // 创建限流节点
                fallbackZk.createPersistent(FREQ_PATH + "/" + serverName, null);
                // 创建白名单节点
                fallbackZk.createPersistent(WHITELIST_PATH + "/" + serverName, null);
            } else {
                // 创建  zk  runtime imstances
                masterZk.createPersistent(RUNTIME_PATH + "/" + serverName, null);
                // 创建  zk  config 服务 持久节点 \
                masterZk.createPersistent(CONFIG_PATH + "/" + serverName, null);
                // 创建路由节点
                masterZk.createPersistent(ROUTES_PATH + "/" + serverName, null);
                // 创建限流节点
                masterZk.createPersistent(FREQ_PATH + "/" + serverName, null);
                // 创建白名单节点
                masterZk.createPersistent(WHITELIST_PATH + "/" + serverName, null);
            }
        } catch (Exception ex) {
            logger.error("[createPreparePath] ==> create zk node failed...", ex, ex);
            logger.error(ex.getMessage(), ex);
        }
    }


    /**
     * 获得 服务集合
     *
     * @return
     */
    private List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        if (processorMap == null) {
            return null;
        }
        Set<ProcessorKey> keys = processorMap.keySet();
        for (ProcessorKey key : keys) {
            SoaServiceDefinition<?> processor = processorMap.get(key);
            if (processor.ifaceClass != null) {
                Service service = processor.ifaceClass.getAnnotation(Service.class);
                services.add(service);
            }
        }
        return services;
    }

}
