package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.Service;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.registry.*;
import com.github.dapeng.route.Route;
import com.github.dapeng.route.RouteExecutor;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RegistryAgent using Synchronous zookeeper requesting
 *
 * @author tangliu
 * @date 2016-08-12
 */
public class RegistryAgentImpl implements RegistryAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryAgentImpl.class);

    private final String RUNTIME_PATH = "/soa/runtime/services";
    private final String CONFIG_PATH = "/soa/config/services";
    private final boolean isClient;
    private final ZookeeperRegistry zooKeeperRegistry = new ZookeeperRegistry(this);
    /**
     * 灰度环境下访问生产环境的zk?
     */
    private ZookeeperRegistry zooKeeperMasterClient = null;

    private ZookeeperClient siw, zkfbw;

    private Map<ProcessorKey, SoaServiceDefinition<?>> processorMap;

    public RegistryAgentImpl() {
        this(true);
    }

    public RegistryAgentImpl(boolean isClient) {
        this.isClient = isClient;
    }

    @Override
    public void start() {

        if (!isClient) {
            zooKeeperRegistry.setZookeeperHost(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
            zooKeeperRegistry.connect();

            if (SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_ISCONFIG) {
                zooKeeperMasterClient = new ZookeeperRegistry(this);
                zooKeeperMasterClient.setZookeeperHost(SoaSystemEnvProperties.SOA_ZOOKEEPER_MASTER_HOST);
                zooKeeperMasterClient.connect();
            }
        }
        //todo why?
        siw = new ZookeeperClient(isClient, SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        siw.init();

        if (SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG) {
            zkfbw = new ZookeeperClient(isClient, SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_HOST);
            zkfbw.init();
        }
    }

    @Override
    public void stop() {
        zooKeeperRegistry.destroy();
        if (siw != null) {
            siw.destroy();
        }

        if (zkfbw != null) {
            zkfbw.destroy();
        }
    }

    @Override
    public void registerService(String serverName, String versionName) {
        try {
            String path = RUNTIME_PATH + "/" + serverName + "/" + SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
            String servicePath = RUNTIME_PATH + "/" + serverName;
            String instanceInfo = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;

            RegisterContext registerContext = new RegisterContext(serverName, versionName, servicePath, instanceInfo);

            zooKeeperRegistry.create(path, registerContext, true);
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

        /*if (processorMap == null) {
            return;
        }
        Set<ProcessorKey> keys = processorMap.keySet();
        for (ProcessorKey key : keys) {
            SoaServiceDefinition<?> processor = processorMap.get(key);

            if (processor.ifaceClass != null) {
                Service service = processor.ifaceClass.getAnnotation(Service.class);

                this.registerService(service.name(), service.version());
            }
        }
        //如果开启了全局事务，将事务服务也注册到zookeeper,为了主从竞选，只有主全局事务管理器会执行
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {
            this.registerService("com.github.dapeng.transaction.api.service.GlobalTransactionService", "1.0.0");
        }*/
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
    public ServiceInfos loadMatchedServices(String serviceName, String versionName, boolean compatible) {

        boolean usingFallbackZookeeper = false;
        List<ServiceInfo> serviceInfos = siw.getServiceInfo(serviceName, versionName, compatible);
        if (serviceInfos.size() <= 0 && SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG) {
            usingFallbackZookeeper = true;
            serviceInfos = zkfbw.getServiceInfo(serviceName, versionName, compatible);
        }

        //使用路由规则，过滤可用服务器 （local模式不考虑）
        final boolean isLocal = "local".equals(SoaSystemEnvProperties.SOA_REMOTING_MODE);
        if (!isLocal) {
            InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();
            List<Route> routes = usingFallbackZookeeper ? zkfbw.getRoutes() : siw.getRoutes();
            List<ServiceInfo> tmpList = new ArrayList<>();

            for (ServiceInfo sif : serviceInfos) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(sif.host);
                    if (RouteExecutor.isServerMatched(context, routes, inetAddress)) {
                        tmpList.add(sif);
                    }
                } catch (UnknownHostException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            //LOGGER.debug("路由过滤前可用列表{}", serviceInfos.stream().map(s -> s.getHost()).collect(Collectors.toList()));
            serviceInfos = tmpList;
            //LOGGER.debug("路由过滤后可用列表{}", serviceInfos.stream().map(s -> s.getHost()).collect(Collectors.toList()));
        }

        return new ServiceInfos(usingFallbackZookeeper, serviceInfos);
    }

    @Override
    public Map<ConfigKey, Object> getConfig(boolean usingFallback, String serviceKey) {
        return null;
    }

    @Override
    public List<Route> getRoutes(boolean usingFallback) {
        return null;
    }

    /**
     * @param configs
     * @param serverName
     * @param versionName
     * @author hz.lei
     */
    @Override
    public void registerConfig(ZkNodeConfigContext configs, String serverName, String versionName) {
        try {
            String path = CONFIG_PATH + "/" + serverName + "/" + SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT + ":" + versionName;
//            zooKeeperRegistry.addOrUpdateConfigNode(path, configs);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    @Override
    public void registerAllConfig(ZkNodeConfigContext configs) {
        List<Service> services = getAllServices();
        if (services == null) {
            return;
        }
        services.forEach(service -> {
            registerConfig(configs, service.name(), service.version());
        });
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
