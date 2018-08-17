package com.github.dapeng.client.netty;

import com.github.dapeng.core.*;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.zookeeper.ClientZkAgent;
import com.github.dapeng.registry.zookeeper.ClientZkAgentImpl;
import com.github.dapeng.registry.zookeeper.LoadBalanceAlgorithm;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.github.dapeng.core.SoaCode.*;

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

        ClientInfoWeakRef(SoaConnectionPool.ClientInfo referent,
                          ReferenceQueue<? super SoaConnectionPool.ClientInfo> q) {
            super(referent, q);
            this.serviceName = referent.serviceName;
            this.version = referent.version;
        }
    }

    private Map<String, ZkServiceInfo> zkInfos = new ConcurrentHashMap<>();
    private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();
    private ClientZkAgent zkAgent = new ClientZkAgentImpl();

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
                ZkServiceInfo zkServiceInfo = zkInfos.remove(clientInfoRef.serviceName);

                if (zkServiceInfo != null) {
                    zkAgent.cancelSyncService(zkServiceInfo);
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

            ZkServiceInfo zkInfo = new ZkServiceInfo(serviceName, new ArrayList<>());
            zkAgent.syncService(zkInfo);

            if (zkInfo.getStatus() == ZkServiceInfo.Status.ACTIVE) {
                zkInfos.put(serviceName, zkInfo);
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
        // 选好的服务版本(可能不同于请求的版本)
        String serverVersion = InvocationContextImpl.Factory.currentInstance().versionName();
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }
        long timeout = getTimeout(service, serverVersion, method);
        if (logger.isDebugEnabled()) {
            logger.debug("findConnection:serviceName:{},methodName:{},version:[{} -> {}] ,TimeOut:{}", service, method, version, serverVersion, timeout);
        }
        return connection.send(service, serverVersion, method, request, requestSerializer, responseSerializer, timeout);
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service,
                                              String version,
                                              String method,
                                              REQ request,
                                              BeanSerializer<REQ> requestSerializer,
                                              BeanSerializer<RESP> responseSerializer) throws SoaException {

        SoaConnection connection = findConnection(service, version, method);
        String serverVersion = InvocationContextImpl.Factory.currentInstance().versionName();
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }
        long timeout = getTimeout(service, serverVersion, method);
        if (logger.isDebugEnabled()) {
            logger.debug("findConnection:serviceName:{},methodName:{},version:[{} -> {}] ,TimeOut:{}", service, method, version, serverVersion, timeout);
        }
        return connection.sendAsync(service, serverVersion, method, request, requestSerializer, responseSerializer, timeout);
    }

    private SoaConnection findConnection(final String service,
                                         final String version,
                                         final String method) throws SoaException {
        ZkServiceInfo zkInfo = zkInfos.get(service);

        if (zkInfo == null || zkInfo.getStatus() != ZkServiceInfo.Status.ACTIVE) {
            //todo should find out why zkInfo is null
            // 1. target service not exists
            logger.error(getClass().getSimpleName() + "::findConnection-0[service: " + service + "], zkInfo not found, now reSyncService");

            zkInfo = new ZkServiceInfo(service, new ArrayList<>());
            zkAgent.syncService(zkInfo);
            if (zkInfo.getStatus() != ZkServiceInfo.Status.ACTIVE) {
                logger.error(getClass().getSimpleName() + "::findConnection-1[service: " + service + "], zkInfo not found");
                return null;
            }

            zkInfos.put(service, zkInfo);
        }
        //当zk上服务节点发生变化的时候, 会导致这里拿不到服务运行时实例.
        //目前简单重试三次处理
        List<RuntimeInstance> compatibles = retryGetConnection(zkInfo);
        if (compatibles == null || compatibles.isEmpty()) {
            return null;
        }

        // checkVersion
        List<RuntimeInstance> checkVersionInstances = compatibles.stream().filter(rt -> checkVersion(version, rt.version)).collect(Collectors.toList());
        if (checkVersionInstances == null || checkVersionInstances.isEmpty()) {
            logger.error(getClass().getSimpleName() + "::findConnection[service: " + service + ":" + version + "], not found available version of instances");
            throw new SoaException(NoMatchedService, "服务 [ " + service + ":" + version + "] 无可用实例:没有找到对应的服务版本");
        }

        // router
        List<RuntimeInstance> routedInstances = router(service, method, version, checkVersionInstances);
        if (routedInstances == null || routedInstances.isEmpty()) {
            logger.error(getClass().getSimpleName() + "::findConnection[service: " + service + "], not found available instances by routing rules");
            throw new SoaException(NoMatchedRouting, "服务 [ " + service + " ] 无可用实例:路由规则没有解析到可运行的实例");
        }

        //loadBalance
        RuntimeInstance inst = loadBalance(service, version, method, routedInstances);
        if (inst == null) {
            // should not reach here
            throw new SoaException(NotFoundServer, "服务 [ " + service + " ] 无可用实例:负载均衡没有找到合适的运行实例");
        }

        inst.increaseActiveCount();

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

        // TODO: 2018-08-04  服务端需要返回来正确的版本号
        InvocationContextImpl context = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        context.versionName(inst.version);
        return subPool.getConnection();
    }

    /**
     * 版本 兼容(主版本不兼容，副版本向下兼容)
     *
     * @param reqVersion
     * @param targetVersion
     * @return
     */
    private boolean checkVersion(String reqVersion, String targetVersion) {
        String[] reqArr = reqVersion.split("[.]");
        String[] tarArr = targetVersion.split("[.]");
        if (Integer.parseInt(tarArr[0]) != Integer.parseInt(reqArr[0])) {
            return false;
        }
        return ((Integer.parseInt(tarArr[1]) * 10 + Integer.parseInt(tarArr[2]))
                >= (Integer.parseInt(reqArr[1]) * 10 + Integer.parseInt(reqArr[2])));
    }

    /**
     * 如果出现异常（ConcurrentModifyException）,或获取到的实例为0，进行重试
     *
     * @param zkInfo
     */
    private List<RuntimeInstance> retryGetConnection(ZkServiceInfo zkInfo) {
        int retry = 1;
        do {
            try {
                List<RuntimeInstance> runtimeInstances = zkInfo.getRuntimeInstances();
                if (runtimeInstances.size() > 0) {
                    return runtimeInstances;
                }
            } catch (Exception e) {
                logger.error("zkInfo get connection 出现异常: " + e.getMessage());
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        } while (retry++ <= 3);
        logger.warn("retryGetConnection::重试3次获取 connection 失败");
        return null;
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
    private List<RuntimeInstance> router(String service, String method, String version, List<RuntimeInstance> compatibles) throws SoaException {
        InvocationContextImpl context = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<Route> routes = zkAgent.getRoutes(service);
        if (routes == null || routes.size() == 0) {
            logger.debug("router 获取 路由信息为空或size为0,跳过router,服务实例数：{}", compatibles.size());
            return compatibles;
        } else {
            context.serviceName(service);
            context.methodName(method);
            context.versionName(version);
            List<RuntimeInstance> runtimeInstances = RoutesExecutor.executeRoutes(context, routes, compatibles);
            if (runtimeInstances.size() == 0) {
                throw new SoaException(SoaCode.NoMatchedRouting);
            }
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

        ZkServiceInfo zkServiceInfo = zkInfos.get(serviceName);
        //方法级别
        LoadBalanceStrategy methodLB = null;
        //服务配置
        LoadBalanceStrategy serviceLB = null;
        //全局
        LoadBalanceStrategy globalLB = null;

        if (zkServiceInfo != null) {
            //方法级别
            methodLB = zkServiceInfo.loadbalanceConfig.serviceConfigs.get(methodName);
            //服务配置
            serviceLB = zkServiceInfo.loadbalanceConfig.serviceConfigs.get(ConfigKey.LoadBalance.getValue());
            //全局
            globalLB = zkServiceInfo.loadbalanceConfig.globalConfig;
        }

        InvocationContext invocationContext = InvocationContextImpl.Factory.currentInstance();
        LoadBalanceStrategy balance = invocationContext.loadBalanceStrategy().orElse(null);

        if (balance == null) {
            if (methodLB != null) {
                balance = methodLB;
            } else if (serviceLB != null) {
                balance = serviceLB;
            } else if (globalLB != null) {
                balance = globalLB;
            } else {
                balance = DEFAULT_LB_STRATEGY;
                invocationContext.loadBalanceStrategy(balance);
            }
        }

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
    private long getTimeout(String service, String version, String method) {

        long maxTimeout = SoaSystemEnvProperties.SOA_MAX_TIMEOUT;
        long defaultTimeout = SoaSystemEnvProperties.SOA_DEFAULT_TIMEOUT;

        Optional<Integer> invocationTimeout = getInvocationTimeout();
        Optional<Long> envTimeout = SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT.longValue() == 0 ?
                Optional.empty() : Optional.of(SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT.longValue());

        Optional<Long> zkTimeout = getZkTimeout(service, version, method);
        Optional<Long> idlTimeout = getIdlTimeout(service, version, method);

        Optional<Long> timeout;
        if (invocationTimeout.isPresent()) {
            timeout = invocationTimeout.map(Long::valueOf);
        } else if (envTimeout.isPresent()) {
            timeout = envTimeout;
        } else if (idlTimeout.isPresent()) {
            timeout = idlTimeout;
        } else if (zkTimeout.isPresent()) {
            timeout = zkTimeout;
        } else {
            timeout = Optional.of(defaultTimeout);
        }

        return timeout.get() >= maxTimeout ? maxTimeout : timeout.get();

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
     * <p>
     * method level -> service level -> global level
     * <</p>
     *
     * @return
     */
    private Optional<Long> getZkTimeout(String serviceName, String version, String methodName) {
        ZkServiceInfo configInfo = zkInfos.get(serviceName);
        //方法级别
        Long methodTimeOut = null;
        //服务配置
        Long serviceTimeOut = null;

        Long globalTimeOut = null;

        if (configInfo != null) {
            //方法级别
            methodTimeOut = configInfo.timeConfig.serviceConfigs.get(methodName);
            //服务配置
            serviceTimeOut = configInfo.timeConfig.serviceConfigs.get(ConfigKey.TimeOut.getValue());

            globalTimeOut = configInfo.timeConfig.globalConfig;
        }

        /*logger.debug("request:serviceName:{},methodName:{},version:{}," +
                        " methodTimeOut:{},serviceTimeOut:{},globalTimeOut:{}",
                serviceName, methodName, version, methodTimeOut, serviceTimeOut, globalTimeOut);*/

        if (methodTimeOut != null) {

            return Optional.of(methodTimeOut);
        } else if (serviceTimeOut != null) {

            return Optional.of(serviceTimeOut);
        } else if (globalTimeOut != null) {

            return Optional.of(globalTimeOut);
        } else {
            return Optional.empty();
        }
    }
}