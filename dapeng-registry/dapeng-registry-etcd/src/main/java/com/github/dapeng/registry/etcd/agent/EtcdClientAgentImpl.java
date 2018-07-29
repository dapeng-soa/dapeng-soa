package com.github.dapeng.registry.etcd.agent;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.RegisterInfo;
import com.github.dapeng.registry.RegistryClientAgent;
import com.github.dapeng.registry.etcd.EtcdClientRegistry;
import com.github.dapeng.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class EtcdClientAgentImpl implements RegistryClientAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClientAgentImpl.class);

    private EtcdClientRegistry etcdClient;

    /**
     * 路由配置信息
     */
    private final Map<String, List<Route>> routesMap = new ConcurrentHashMap<>(16);


    public EtcdClientAgentImpl() {
        start();
    }

    @Override
    public void start() {
        etcdClient = new EtcdClientRegistry(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        etcdClient.init();
    }


    /**
     * 优雅退出的时候, 需要调用这个
     */
    @Override
    public void stop() {
        if (etcdClient != null) {
            etcdClient.destroy();
        }
    }


    @Override
    public void cancelSyncService(RegisterInfo zkInfo) {
        LOGGER.info("cancelSyncService:[" + zkInfo.service + "]");
        zkInfo.setStatus(RegisterInfo.Status.CANCELED);
    }

    @Override
    public void syncService(RegisterInfo zkInfo) {
        if (zkInfo.getStatus() != RegisterInfo.Status.ACTIVE) {
            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + "]:zkInfo just created, now sync with zk");
            etcdClient.syncServiceInfo(zkInfo);


            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + ", status:" + zkInfo.getStatus() + "]");
        }

        //使用路由规则，过滤可用服务器
        // fixme 在runtime跟config变化的时候才需要计算可用节点信息
        InvocationContext context = InvocationContextImpl.Factory.currentInstance();

        if (zkInfo.getStatus() == RegisterInfo.Status.ACTIVE && zkInfo.getRuntimeInstances() != null) {

            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + "]:zkInfo succeed");
        } else {
            LOGGER.info(getClass().getSimpleName() + "::syncService[serviceName:" + zkInfo.service + "]:zkInfo failed");
        }
    }

    @Override
    public List<Route> getRoutes(String service) {
        return etcdClient.getRoutes(service);
    }
}
