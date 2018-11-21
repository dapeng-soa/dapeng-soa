package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.*;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.dapeng.registry.zookeeper.CommonZk.*;

/**
 * RegistryAgent using Synchronous zookeeper requesting
 *
 * @author tangliu
 * @date 2016-08-12
 */
public class ServerZkAgentImpl implements RegistryAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZkAgentImpl.class);

    private static final RegistryAgent instance = new ServerZkAgentImpl();

    private final ServerZk serverZk = new ServerZk(this);
    /**
     * 灰度环境下访问生产环境的zk?
     */
    private ServerZk zooKeeperMasterClient = null;


    private Map<ProcessorKey, SoaServiceDefinition<?>> processorMap;


    private ServerZkAgentImpl() {
    }

    public static RegistryAgent getInstance() {
        return instance;
    }

    @Override
    public void start() {

        serverZk.setZookeeperHost(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        serverZk.connect();

        if (SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_ISCONFIG) {
            zooKeeperMasterClient = new ServerZk(this);
            zooKeeperMasterClient.setZookeeperHost(SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_HOST);
            zooKeeperMasterClient.connect();
        }
    }

    @Override
    public void stop() {
        serverZk.destroy();
    }

    @Override
    public void unregisterService(String serverName, String versionName) {
        try {
            //fixme
            String path = "/soa/runtime/services/" + serverName;
            String instPath =  SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
            LOGGER.info(" logger zookeeper unRegister service: " + path);
            List<String> children = serverZk.zk.getChildren(path, false);
            children.forEach(child -> {
                if(child.contains(instPath)){
                    try {
                        String fullPath = path + "/" + child;
                        serverZk.zk.delete(fullPath,-1);
                    } catch (InterruptedException | KeeperException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void registerService(String serverName, String versionName) {
        try {
            String path = RUNTIME_PATH + "/" + serverName + "/" + SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
            String servicePath = RUNTIME_PATH + "/" + serverName;
            String instanceInfo = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;

            RegisterContext registerContext = new RegisterContext(serverName, versionName, servicePath, instanceInfo);
            if (ContainerFactory.getContainer().status() == Container.STATUS_SHUTTING
                    || ContainerFactory.getContainer().status() == Container.STATUS_DOWN) {
                LOGGER.warn("Container is shutting down");
                return;
            }
            // 注册服务 runtime 实例 到 zk
            serverZk.create(path, "", registerContext, true);

            // 创建  zk  config 服务 持久节点  eg:  /soa/config/com.github.dapeng.soa.UserService
            serverZk.create(CONFIG_PATH + "/" + serverName, "", null, false);

            // 创建路由节点
            serverZk.create(ROUTES_PATH + "/" + serverName, "", null, false);

            // 创建限流节点
            serverZk.create(FREQ_PATH + "/" + serverName, "",null, false);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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

    @Override
    public void setProcessorMap(Map<ProcessorKey, SoaServiceDefinition<?>> processorMap) {
        this.processorMap = processorMap;
    }

    @Override
    public Map<ProcessorKey, SoaServiceDefinition<?>> getProcessorMap() {
        return this.processorMap;
    }

    /**
     * 根据serviceKey拿到 zk config 信息
     *
     * @param usingFallback
     * @param serviceKey
     * @return
     */
    @Override
    public ZkServiceInfo getZkServiceInfo(boolean usingFallback, String serviceKey) {
        return serverZk.getZkServiceInfo(serviceKey);
    }

    /**
     * getAllServices
     *
     * @return
     */
    public List<Service> getAllServices() {
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
        //如果开启了全局事务，将事务服务也注册到zookeeper,为了主从竞选，只有主全局事务管理器会执行
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {
            this.registerService("com.github.dapeng.transaction.api.service.GlobalTransactionService", "1.0.0");
        }
        return services;
    }


}
