package com.github.dapeng.registry.etcd.agent;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.RegisterInfo;
import com.github.dapeng.registry.RegistryServerAgent;
import com.github.dapeng.registry.etcd.EtcdServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * desc: etcd registry
 *
 * @author hz.lei
 * @since 2018年07月19日 下午5:24
 */
public class EtcdRegistryAgentImpl implements RegistryServerAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdRegistryAgentImpl.class);

    private final String RUNTIME_PATH = "/soa/runtime/services";
    private final String CONFIG_PATH = "/soa/config/services";
    private final static String ROUTES_PATH = "/soa/config/routes";

    private final EtcdServerRegistry etcdServerRegistry = new EtcdServerRegistry(this);

    private Map<ProcessorKey, SoaServiceDefinition<?>> processorMap;


    @Override
    public void start() {
        etcdServerRegistry.setEtcdRegisterHost(SoaSystemEnvProperties.SOA_ETCD_HOST);
        etcdServerRegistry.connect();
    }

    @Override
    public void stop() {
        etcdServerRegistry.destroy();
    }

    @Override
    public void unregisterService(String serverName, String versionName) {
        try {
            //fixme
            String path = "/soa/runtime/services/" + serverName + "/" + SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
            LOGGER.info(" logger zookeeper unRegister service: " + path);
//            serverZk.zk.delete(path, -1);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void registerService(String serverName, String versionName) {
        try {
            String instancePath = MessageFormat.format("{0}/{1}/{2}:{3}:{4}", RUNTIME_PATH, serverName, SoaSystemEnvProperties.SOA_CONTAINER_IP, SoaSystemEnvProperties.SOA_CONTAINER_PORT, versionName);
            String servicePath = MessageFormat.format("{0}/{1}", RUNTIME_PATH, serverName);

            if (ContainerFactory.getContainer().status() == Container.STATUS_SHUTTING
                    || ContainerFactory.getContainer().status() == Container.STATUS_DOWN) {
                LOGGER.warn("Container is shutting down");
                return;
            }
            // 注册服务 runtime 实例 到 etcd
            etcdServerRegistry.register(servicePath, instancePath);
            // 创建  etcd  config 服务 持久节点  eg:  /soa/config/com.github.dapeng.soa.UserService
            etcdServerRegistry.register(MessageFormat.format("{0}/{1}", CONFIG_PATH, serverName), null);
            // 创建路由节点 etcd
            etcdServerRegistry.register(MessageFormat.format("{0}/{1}", ROUTES_PATH, serverName), null);
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
    public RegisterInfo getConfig(boolean usingFallback, String serviceKey) {
        return etcdServerRegistry.getConfigData(serviceKey);
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
