package com.github.dapeng.client.netty;


import com.github.dapeng.zookeeper.common.ConfigKey;
import com.github.dapeng.zookeeper.common.LoadBalanceAlgorithm;
import com.github.dapeng.zookeeper.common.ZkConfig;
import com.github.dapeng.zookeeper.common.ZkServiceInfo;
import com.github.dapeng.core.*;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import com.github.dapeng.zookeeper.agent.ClientZkAgent;
import com.github.dapeng.zookeeper.agent.impl.ClientZkAgentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author lihuimin
 * @date 2017/12/22
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {
    private final Logger logger = LoggerFactory.getLogger(SoaConnectionPoolImpl.class);
    private final LoadBalanceStrategy DEFAULT_LB_STRATEGY = LoadBalanceStrategy.Random;

    class ClientInfoWeakRef extends WeakReference<SoaConnectionPool.ClientInfo> {
        final String serviceName;
        final String version;

        public ClientInfoWeakRef(SoaConnectionPool.ClientInfo referent,
                                 ReferenceQueue<? super SoaConnectionPool.ClientInfo> q) {
            super(referent, q);
            this.serviceName = referent.serviceName;
            this.version = referent.version;
        }
    }

    private Map<String, ZkServiceInfo> zkServiceInfoMap = new ConcurrentHashMap<>();
    private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();
    private static ClientZkAgent clientZkAgent = ClientZkAgentImpl.getClientZkAgentInstance();

    private ReentrantLock subPoolLock = new ReentrantLock();

    private Map<String, ClientInfoWeakRef> clientInfos = new ConcurrentHashMap<>(16);
    private final ReferenceQueue<ClientInfo> referenceQueue = new ReferenceQueue<>();

    Thread cleanThread = new Thread(() -> {
        while (true) {
            try {
                ClientInfoWeakRef clientInfoRef = (ClientInfoWeakRef) referenceQueue.remove(1000);
                if (clientInfoRef == null) continue;
                final String key = clientInfoRef.serviceName + ":" + clientInfoRef.version;
                if (logger.isDebugEnabled()) {
                    logger.debug("client for service:" + key + " is gone.");
                }
                clientInfos.remove(key);
                ZkServiceInfo zkServiceInfo = zkServiceInfoMap.remove(clientInfoRef.serviceName);
                if (zkServiceInfo != null) {
                    clientZkAgent.cancelService(zkServiceInfo);
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }, "dapeng-zk-monitor-thread");


    public SoaConnectionPoolImpl() {
        IdleConnectionManager connectionManager = new IdleConnectionManager();
        connectionManager.start();

        cleanThread.setDaemon(true);
        cleanThread.start();
    }


    private boolean checkVersion(String reqVersion, String targetVersion) {
        // x.y.z
        // x.Y.Z Y.Z >= y.z
        //TODU  实现version兼容
        return true;
    }

    @Override
    public synchronized ClientInfo registerClientInfo(String serviceName, String version) {
        final String key = serviceName + ":" + version;
        ClientInfoWeakRef clientInfoRef = clientInfos.get(key);
        ClientInfo clientInfo = (clientInfoRef == null) ? null : clientInfoRef.get();
        if (clientInfo != null) {
            return clientInfo;
        } else {
            clientInfo = new ClientInfo(serviceName, version);
            clientInfoRef = new ClientInfoWeakRef(clientInfo, referenceQueue);
            clientInfos.put(key, clientInfoRef);

            ZkServiceInfo zkServiceInfo = new ZkServiceInfo(serviceName);
            clientZkAgent.syncService(zkServiceInfo);
            if (zkServiceInfo.getStatus() == ZkServiceInfo.Status.ACTIVE) {
                zkServiceInfoMap.put(serviceName, zkServiceInfo);
            } else {
                //todo ??
            }
            return clientInfo;
        }

    }

    @Override
    public <REQ, RESP> RESP send(String service, String version,
                                 String method, REQ request,
                                 BeanSerializer<REQ> requestSerializer,
                                 BeanSerializer<RESP> responseSerializer)
            throws SoaException {
        SoaConnection connection = findConnection(service, version, method);
       /* if (connection == null) {
            throw new SoaException("Err-Core-098", "服务 [ " + service + " ] 无可用实例");
        }*/
        long timeout = getTimeout(service, version, method);
        return connection.send(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service,
                                              String version,
                                              String method,
                                              REQ request,
                                              BeanSerializer<REQ> requestSerializer,
                                              BeanSerializer<RESP> responseSerializer) throws SoaException {

        SoaConnection connection = findConnection(service, version, method);
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer);
        }
        long timeout = getTimeout(service, version, method);
        return connection.sendAsync(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    private SoaConnection findConnection(String service, String version, String method) throws SoaException {
        ZkServiceInfo zkServiceInfo = zkServiceInfoMap.get(service);

        if (zkServiceInfo == null || zkServiceInfo.getStatus() != ZkServiceInfo.Status.ACTIVE) {
            //todo should find out why zkServiceInfo is null
            // 1. target service not exists
            logger.error(getClass().getSimpleName() + "::findConnection-0[service: " + service + "], zkServiceInfo not found, now resyncService");

            zkServiceInfo = new ZkServiceInfo(service);
            clientZkAgent.syncService(zkServiceInfo);
            if (zkServiceInfo.getStatus() != ZkServiceInfo.Status.ACTIVE) {
                logger.error(getClass().getSimpleName() + "::findConnection-1[service: " + service + "], not found available runtime instances");
                throw new SoaException("Err-Core-098", "服务 [ " + service + " ] 无可用实例");
                //return null;
            }
            zkServiceInfoMap.put(service, zkServiceInfo);
        }

        //版本兼容 判断
        List<RuntimeInstance> compatibles = clientZkAgent.getZkClient().getRuntimeInstances(service).stream().filter(rt -> checkVersion(version, rt.version)).collect(Collectors.toList());
        if (compatibles == null || compatibles.isEmpty()) {
            logger.error(getClass().getSimpleName() + "::findConnection[service: " + service + "], not found available running version[{}] instances", version);
            throw new SoaException("Err-Core-098", "服务 [ " + service + " ] 无可用实例:没有可以运行的服务版本[" + version + "]");
            //return null;
        }

        // 可用服务实例 经过路由规则进行过滤
        List<RuntimeInstance> routedInstances = router(service, method, version, compatibles);
        if (routedInstances == null || routedInstances.isEmpty()) {
            logger.error(getClass().getSimpleName() + "::findConnection[service: " + service + "], not found available instances by routing rules");
            throw new SoaException("Err-Core-098", "服务 [ " + service + " ] 无可用实例:路由规则没有解析到可运行的实例");
            //return null;
        }
        RuntimeInstance inst = loadBalance(service, version, method, routedInstances);
        if (inst == null) {
            logger.error(getClass().getSimpleName() + "::findConnection[service:" + service + "], the instance is loss after loadBalance[{}]...");
            throw new SoaException("Err-Core-098", "服务 [ " + service + " ] 无可用实例:负载均衡没有找到合适的运行实例");
            //return null;
        }

        //调用次数加 1
        //inst.getActiveCount().incrementAndGet();
        clientZkAgent.activeCountIncrement(inst);

        IpPort ipPort = new IpPort(inst.ip, inst.port);
        SubPool subPool = subPools.get(ipPort);
        if (subPool == null) {
            try {
                subPoolLock.lock();
                subPool = subPools.get(ipPort);
                if (subPool == null) {
                    subPool = new SubPool(inst.ip, inst.port);
                    subPools.put(ipPort, subPool);
                }
            } finally {
                subPoolLock.unlock();
            }
        }
        return subPool.getConnection();
    }

    /**
     * 服务路由
     *
     * @param service
     * @param method
     * @param version
     * @param compatibles
     * @return
     */
    private List<RuntimeInstance> router(String service, String method, String version, List<RuntimeInstance> compatibles) {
        InvocationContextImpl context = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<Route> routes = clientZkAgent.getZkClient().getRoutes(service);
        if (routes == null || routes.size() == 0) {
            logger.debug("router 获取 路由信息为空或size为0,跳过router,服务实例数：{}", compatibles.size());
            return compatibles;
        } else {
            context.serviceName(service);
            context.methodName(method);
            context.versionName(version);
            List<RuntimeInstance> runtimeInstances = RoutesExecutor.executeRoutes(context, routes, compatibles);
            return runtimeInstances;
        }
    }


    /**
     * 根据zk 负载均衡配置解析，分为 全局/service级别/method级别
     *
     * @param serviceName
     * @param version
     * @param methodName
     * @param compatibles
     * @return
     */
    private RuntimeInstance loadBalance(String serviceName, String version, String methodName, List<RuntimeInstance> compatibles) {
        InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
        LoadBalanceStrategy balance = invocationContext.loadBalanceStrategy().orElse(null);
        if (balance == null) {
            balance = LoadBalanceStrategy.findByValue((String) clientZkAgent.getZkClient().getServiceConfig(serviceName, ConfigKey.LoadBalance, methodName, DEFAULT_LB_STRATEGY.name()));
        }
        invocationContext.loadBalanceStrategy(balance);
        if (logger.isDebugEnabled()) {
            logger.debug("request loadBalance strategy is {}", balance);
        }
        RuntimeInstance instance = null;

        switch (balance) {
            case Random:
                instance = LoadBalanceAlgorithm.random(compatibles);
                break;
            case RoundRobin:
                instance = LoadBalanceAlgorithm.roundRobin(compatibles);
                break;
            case LeastActive:
                instance = LoadBalanceAlgorithm.leastActive(compatibles);
                break;
            case ConsistentHash:
                //TODO
                break;
            default:
                // won't be here
        }
        logger.info(getClass().getSimpleName() + "::loadBalance[service:" + serviceName + "],loadBalance by [{}]...", balance.name());
        return instance;
    }

    /**
     * 超时逻辑:
     * 1. 如果invocationContext有设置的话, 那么用invocationContext的(这个值每次调用都可能不一样)
     * 2. invocationContext没有的话, 就拿Option的(命令行或者环境变量)
     * 3. 没设置Option的话, 那么取ZK的.
     * 4. ZK没有的话, 拿IDL的(暂没实现该参数)
     * 5. 都没有的话, 拿默认值.(这个值所有方法一致, 假设为50S)
     * <p>
     * 最后校验一下,拿到的值不能超过系统设置的最大值
     *
     * @param service
     * @param version
     * @param method
     * @return
     */
    private long getTimeout(String serviceName, String version, String method) {
        long defaultTimeout = SoaSystemEnvProperties.SOA_DEFAULT_TIMEOUT;
        long maxTimeout = SoaSystemEnvProperties.SOA_MAX_TIMEOUT;

        Optional<Integer> invocationTimeout = getInvocationTimeout();
        if (invocationTimeout.isPresent()) {
            return invocationTimeout.get() >= maxTimeout ? maxTimeout : invocationTimeout.get();
        }
        Optional<Long> envTimeout = SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT == 0 ? Optional.empty() : Optional.of(SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT);
        if (envTimeout.isPresent()) {
            return envTimeout.get() >= maxTimeout ? maxTimeout : envTimeout.get();
        }

        Optional<Long> zkTimeout = getZkTimeout(serviceName, version, method);
        if (zkTimeout.isPresent()) {
            return zkTimeout.get() >= maxTimeout ? maxTimeout : zkTimeout.get();
        }

        //TODO  拿IDL的(暂没实现该参数)
        Optional<Long> idlTimeout = getIdlTimeout(serviceName, version, method);
        if (idlTimeout.isPresent()) {
            return idlTimeout.get() >= maxTimeout ? maxTimeout : idlTimeout.get();
        }

        return defaultTimeout;
    }

    private Optional<Integer> getInvocationTimeout() {
        InvocationContext context = InvocationContextImpl.Factory.currentInstance();
        return context.timeout();
    }

    /**
     * 获取服务Idl timeout
     *
     * @return
     */
    private Optional<Long> getIdlTimeout(String serviceName, String version, String methodName) {
        Optional<Long> timeout = Optional.empty();

//        Application application = ContainerFactory.getContainer().getApplication(new ProcessorKey(serviceName, version));
//        if (application != null) {
//            Optional<ServiceInfo> serviceInfo = application.getServiceInfo(serviceName, version);
//            if (serviceInfo.isPresent()) {
//                // class config
//                Optional<CustomConfigInfo> classConfigInfo = serviceInfo.get().configInfo;
//                // method config map
//                Map<String, Optional<CustomConfigInfo>> methodsConfigMap = serviceInfo.get().methodsMap;
//                // detail method config
//                Optional<CustomConfigInfo> methodConfigInfo = methodsConfigMap.get(methodName);
//                //方法级别 配置
//                if (methodConfigInfo.isPresent()) {
//                    timeout = Optional.of(methodConfigInfo.get().timeout);
//                    //类级别配置
//                } else if (classConfigInfo.isPresent()) {
//                    timeout = Optional.of(classConfigInfo.get().timeout);
//                }
//            }
//        }
        return timeout;
    }

    /**
     * 获取 zookeeper timeout config
     * method level -> service level -> global level
     *
     * @return
     */
    private Optional<Long> getZkTimeout(String serviceName, String version, String methodName) {
        return Optional.of(ZkConfig.timeHelper((String) clientZkAgent.getZkClient().getServiceConfig(serviceName, ConfigKey.TimeOut, methodName, null)));
    }
}