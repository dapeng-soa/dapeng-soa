package com.github.dapeng.client.netty;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.LoadBalanceStrategy;
import com.github.dapeng.registry.RuntimeInstance;
import com.github.dapeng.registry.zookeeper.*;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.zookeeper.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Reference;
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

/**
 * @author lihuimin
 * @date 2017/12/22
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {
    private final Logger logger = LoggerFactory.getLogger(SoaConnectionPoolImpl.class);
    private Map<String, ZkServiceInfo> zkInfos = new ConcurrentHashMap<>();
    private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();
    private ZkClientAgent zkAgent = new ZkClientAgentImpl();

    private ReentrantLock subPoolLock = new ReentrantLock();


    private Map<String, WeakReference<ClientInfo>> clientInfos = new ConcurrentHashMap<>(16);
    private Map<WeakReference<ClientInfo>, String> clientInfoRefs = new ConcurrentHashMap<>(16);
    private final ReferenceQueue<ClientInfo> referenceQueue = new ReferenceQueue<>();

    Thread cleanThread = new Thread(() -> {
        while (true) {
            try {
                Reference<ClientInfo> clientInfoRef = (Reference<ClientInfo>) referenceQueue.remove(1000);
                if (clientInfoRef == null) continue;

                String serviceVersion = clientInfoRefs.remove(clientInfoRef);

                if (logger.isDebugEnabled()) {
                    logger.debug("client for service:" + serviceVersion + " is gone.");
                }

                clientInfos.remove(serviceVersion);
                ZkServiceInfo zkServiceInfo = zkInfos.remove(serviceVersion.split(":")[0]);
                zkAgent.cancnelSyncService(zkServiceInfo);
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
        return true;
    }

    @Override
    public synchronized ClientInfo registerClientInfo(String serviceName, String version) {
        final String key = serviceName + ":" + version;

        WeakReference<ClientInfo> clientInfoRef = clientInfos.get(key);
        ClientInfo clientInfo = (clientInfoRef == null) ? null : clientInfoRef.get();
        if (clientInfo != null) {
            //fixme should remove the debug log
            logger.info("registerClientInfo-0:[" + serviceName + ", version:"
                    + version + ", zkInfo:" + zkInfos.get(serviceName));
            return clientInfo;
        } else {
            clientInfo = new ClientInfo(serviceName, version);
            clientInfoRef = new WeakReference<>(clientInfo, referenceQueue);

            clientInfos.put(key, clientInfoRef);
            clientInfoRefs.put(clientInfoRef, key);

            ZkServiceInfo zkInfo = new ZkServiceInfo(serviceName, new ArrayList<>());
            zkAgent.syncService(zkInfo);

            if (zkInfo.getStatus() == ZkServiceInfo.Status.ACTIVE) {
                zkInfos.put(serviceName, zkInfo);
            } else {
                //todo ??
            }
            //fixme should remove the debug log
            logger.info("registerClientInfo-1:[" + serviceName + ", version:"
                    + version + ", zkInfo:" + zkInfos.get(serviceName));
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
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer);
        }
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

    private SoaConnection findConnection(String service,
                                         String version,
                                         String method) {
        ZkServiceInfo zkInfo = zkInfos.get(service);

        if (zkInfo == null) {
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

        List<RuntimeInstance> compatibles = zkInfo.getRuntimeInstances().stream()
                .filter(rt -> checkVersion(version, rt.version))
                .collect(Collectors.toList());
        RuntimeInstance inst = loadBalance(service, version, method, compatibles);
        if (inst == null) {
            logger.error(getClass().getSimpleName() + "::findConnection[service:" + service + "], instance not found");
            return null;
        }

        inst.getActiveCount().incrementAndGet();

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
     * 根据zk 负载均衡配置解析，分为 全局/service级别/method级别
     *
     * @param serviceName
     * @param version
     * @param methodName
     * @param compatibles
     * @return
     */
    private RuntimeInstance loadBalance(String serviceName, String version, String methodName, List<RuntimeInstance> compatibles) {

        ZkConfigInfo configInfo = zkAgent.getConfig(false, serviceName);
        //方法级别
        LoadBalanceStrategy methodLB = configInfo.loadbalanceConfig.serviceConfigs.get(methodName);
        //服务配置
        LoadBalanceStrategy serviceLB = configInfo.loadbalanceConfig.serviceConfigs.get(ConfigKey.LoadBalance.getValue());
        //全局
        LoadBalanceStrategy globalLB = configInfo.loadbalanceConfig.globalConfig;

        LoadBalanceStrategy balance;

        if (methodLB != null) {
            balance = methodLB;
        } else if (serviceLB != null) {
            balance = serviceLB;
        } else if (globalLB != null) {
            balance = globalLB;
        } else {
            balance = LoadBalanceStrategy.Random;
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

        Optional<Long> invocationTimeout = getInvocationTimeout();
        Optional<Long> envTimeout = SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT.longValue() == 0 ?
                Optional.empty() : Optional.of(SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT.longValue());

        Optional<Long> zkTimeout = getZkTimeout(service, version, method);
        Optional<Long> idlTimeout = getIdlTimeout(service, version, method);

        Optional<Long> timeout;
        if (invocationTimeout.isPresent()) {
            timeout = invocationTimeout;
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

    private Optional<Long> getInvocationTimeout() {
        InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();
        return context.getTimeout() == null ? Optional.empty() : context.getTimeout();
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
        ZkConfigInfo configInfo = zkAgent.getConfig(false, serviceName);
        //方法级别
        Long methodTimeOut = configInfo.timeConfig.serviceConfigs.get(methodName);
        //服务配置
        Long serviceTimeOut = configInfo.timeConfig.serviceConfigs.get(ConfigKey.TimeOut.getValue());

        Long globalTimeOut = configInfo.timeConfig.globalConfig;

        logger.debug("request:serviceName:{},methodName:{}," +
                        " methodTimeOut:{},serviceTimeOut:{},globalTimeOut:{}",
                serviceName, methodName, methodTimeOut, serviceTimeOut, globalTimeOut);

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