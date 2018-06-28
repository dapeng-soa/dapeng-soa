package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.FreqControlRule;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.dapeng.registry.zookeeper.CommonZk.*;

/**
 * ServerZkAgent using Synchronous zookeeper requesting
 *
 * @author tangliu
 * @date 2016-08-12
 */
public class ServerZkAgentImpl implements ServerZkAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZkAgentImpl.class);

    private static final ServerZkAgent instance = new ServerZkAgentImpl();

    private final ServerZk serverZk = new ServerZk(this);
    /**
     * 灰度环境下访问生产环境的zk?
     */
    private ServerZk zooKeeperMasterClient = null;


    private Map<ProcessorKey, SoaServiceDefinition<?>> processorMap;


    private ServerZkAgentImpl() {
    }

    public static ServerZkAgent getInstance() {
        return instance;
    }

    @Override
    public void start() {

        serverZk.setZookeeperHost(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        serverZk.init();

        if (SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_ISCONFIG) {
            zooKeeperMasterClient = new ServerZk(this);
            zooKeeperMasterClient.setZookeeperHost(SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_HOST);
            zooKeeperMasterClient.init();
        }
    }

    @Override
    public void stop() {
        serverZk.destroy();
    }

    @Override
    public void unregisterService(String serviceName, String versionName) {
        try {
            //fixme
            String path = "/soa/runtime/services/" + serviceName + "/" + SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
            LOGGER.info(" logger zookeeper unRegister service: " + path);
//            serverZk.zk.delete(path, -1);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void registerService(String serviceName, String versionName) {
        try {
            String instanceInfo = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName + ":1";
            String servicePath = RUNTIME_PATH + "/" + serviceName;
            String path = servicePath + "/" + instanceInfo;

            RegisterContext registerContext = new RegisterContext(serviceName, versionName, servicePath, instanceInfo);
            int containerStatus = ContainerFactory.getContainer().status();
            if (containerStatus == Container.STATUS_SHUTTING
                    || containerStatus == Container.STATUS_DOWN) {
                LOGGER.warn("Container is not running:" + containerStatus);
                return;
            }
            // 注册服务 runtime 实例 到 zk
            serverZk.create(path, registerContext, true);

            // 创建  zk  config 服务 持久节点  eg:  /soa/config/com.github.dapeng.soa.UserService
            serverZk.create(CONFIG_PATH + "/" + serviceName, null, false);

            // 创建路由节点
            serverZk.create(ROUTES_PATH + "/" + serviceName, null, false);

            // 创建限流节点
            serverZk.create(FREQ_PATH + "/" + serviceName, null, false);
            serverZk.getRuntimeInstances(serviceName);
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

        services.forEach(service -> {
            registerService(service.name(), service.version());
        });

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

    @Override
    public List<RuntimeInstance> getRuntimeInstances(String serviceName) {
        return serverZk.getRuntimeInstances(serviceName);
    }

    /**
     * 根据serviceKey拿到 zk config 信息
     *
     * @param usingFallback
     * @param serviceKey
     * @return
     */
    @Override
    public ZkServiceInfo getConfig(boolean usingFallback, String serviceKey) {
        return serverZk.getConfigData(serviceKey);
    }

    @Override
    public List<FreqControlRule> getFreqControlRule(boolean usingFallback, String serviceKey) {
        return serverZk.getFreqControl(serviceKey);
    }

    @Override
    public void pause(String serviceName, String versionName) {
        updateNodeStatus(serviceName, versionName, 0);
    }

    @Override
    public void resume(String serviceName, String versionName) {
        updateNodeStatus(serviceName, versionName, 1);
    }

    private void updateNodeStatus(String serviceName, String versionName, int status) {
        String instanceInfo = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName + ":" + status;
        String servicePath = RUNTIME_PATH + "/" + serviceName;
        String path = servicePath + "/" + instanceInfo;

        RegisterContext registerContext = new RegisterContext(serviceName, versionName, servicePath, instanceInfo);
        serverZk.create(path, registerContext, true);
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
