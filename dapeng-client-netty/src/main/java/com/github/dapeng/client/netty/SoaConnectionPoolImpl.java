package com.github.dapeng.client.netty;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.*;
import com.github.dapeng.core.ServiceInfo;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.registry.*;
import com.github.dapeng.registry.zookeeper.LoadBalanceService;
import com.github.dapeng.registry.zookeeper.ZkClientAgentImpl;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 *
 * @author lihuimin
 * @date 2017/12/22
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private Map<String, ServiceZKInfo> zkInfos = new ConcurrentHashMap<>();
    private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();
    private ZkClientAgent zkAgent = new ZkClientAgentImpl();

    private ReentrantLock subPoolLock = new ReentrantLock();

    private final Logger logger = LoggerFactory.getLogger(SoaConnectionPoolImpl.class);

    //TODO
    List<WeakReference<ClientInfo>> clientInfos;

    // TODO connection idle process.
    Thread cleanThread = null;  // clean idle connections;
    // TODO ClientInfo clean.

    public SoaConnectionPoolImpl() {
        IdleConnectionManager connectionManager = new IdleConnectionManager();
        connectionManager.start();
    }


    private boolean checkVersion(String reqVersion, String targetVersion) {
        // x.y.z
        // x.Y.Z Y.Z >= y.z
        return true;
    }

    @Override
    public ClientInfo registerClientInfo(String serviceName, String version) {
        // TODO
        // clientInfos.add(new WeakReference<ClientInfo>(client));

        zkAgent.syncService(serviceName, zkInfos);
        return null;
    }

    @Override
    public <REQ, RESP> RESP send(String service, String version,
                                 String method, REQ request,
                                 BeanSerializer<REQ> requestSerializer,
                                 BeanSerializer<RESP> responseSerializer)
            throws SoaException {
        ConnectionType connectionType = getConnectionType(requestSerializer);
        SoaConnection connection = findConnection(service, version, method, connectionType);
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

        ConnectionType connectionType = getConnectionType(requestSerializer);
        SoaConnection connection = findConnection(service, version,
                method, connectionType);
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer);
        }
        long timeout = getTimeout(service, version, method);
        return connection.sendAsync(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    private SoaConnection findConnection(String service,
                                         String version,
                                         String method,
                                         ConnectionType connectionType) {
        ServiceZKInfo zkInfo = zkInfos.get(service);

        if (zkInfo == null || zkInfo.getRuntimeInstances() == null){
            return null;
        }
        List<RuntimeInstance> compatibles = zkInfo.getRuntimeInstances().stream()
                .filter(rt -> checkVersion(version, rt.version))
                .collect(Collectors.toList());

        String serviceKey = service + "." + version + "." + method + ".consumer";
        RuntimeInstance inst = loadbalance(serviceKey, compatibles);

        if (inst == null) {
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

        return subPool.getConnection(connectionType);
    }

    private RuntimeInstance loadbalance(String serviceKey, List<RuntimeInstance> compatibles) {

        boolean usingFallbackZookeeper = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG;
        LoadBalanceStratage balance = LoadBalanceStratage.Random;

        Map<ConfigKey, Object> configs = zkAgent.getConfig(usingFallbackZookeeper, serviceKey);
        if (null != configs) {
            balance = LoadBalanceStratage.findByValue((String) configs.get(ConfigKey.LoadBalance));
        }

        RuntimeInstance instance = null;
        switch (balance) {
            case Random:
                instance = LoadBalanceService.random(compatibles);
                break;
            case RoundRobin:
                instance = LoadBalanceService.roundRobin(compatibles);
                break;
            case LeastActive:
                instance = LoadBalanceService.leastActive(compatibles);
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
     *
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
        Optional<Long> envTimeout = SoaSystemEnvProperties.SOA_SERVICE_CLIENT_TIMEOUT.longValue() == 0 ?
                Optional.empty(): Optional.of(SoaSystemEnvProperties.SOA_SERVICE_CLIENT_TIMEOUT.longValue());

        Optional<Long> zkTimeout = getZkTimeout(service,version,method);
        Optional<Long> idlTimeout = getIdlTimeout(service,version,method);

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
        return context.getTimeout() == null ? Optional.empty(): context.getTimeout();
    }

    //TODO

    /**
     * 获取服务Idl timeout
     * @return
     */
    private Optional<Long> getIdlTimeout(String serviceName, String version, String methodName) {
        Optional<Long> timeout = Optional.empty();

        //todo
//        Application application = ContainerFactory.getContainer().getApplication(new ProcessorKey(serviceName,version));
//
//        if (application != null) {
//            Optional<ServiceInfo> serviceInfo = application.getServiceInfo(serviceName,version);
//            if (serviceInfo.isPresent()) {
//                Class<?> service = serviceInfo.get().ifaceClass;
//                List<Method> methods =  Arrays.stream(service.getMethods()).filter(method ->
//                        methodName.equals(method.getName())).collect(Collectors.toList());
//                if (! methods.isEmpty()) {
//                    //TODO: 自定义注解类 (Invocation(timeout=xxx))
//                    Method method = methods.get(0);
//                    try {
//                        Class InvocationClass = service.getClassLoader().loadClass("com.github.dapeng.core.Invocation");
//
//                        if (method.isAnnotationPresent(InvocationClass)) {
//                            Annotation annotation = method.getAnnotation(InvocationClass);
//                            Long annotationTimeout = (Long) annotation.getClass().getDeclaredMethod("timeout").invoke(annotation);
//                            timeout = Optional.of(annotationTimeout);
//                        }
//                    } catch (Exception e) {
//                        logger.error(" Failed to get method: {}:{}:{} invocation Annotation. ", serviceName,version,method);
//                    }
//
//                }
//            }
//        }

        return timeout;

    }

    /**
     *
     * 获取 zookeeper timeout config
     *
     * @param serviceName
     * @param version
     * @param methodName
     * @return
     */
    private Optional<Long> getZkTimeout(String serviceName, String version, String methodName) {
        String serviceKey = serviceName + "." + version + "." + methodName + ".producer";
        Map<ConfigKey, Object> configs = zkAgent.getConfig(false, serviceKey);
        if (null != configs && configs.containsKey(ConfigKey.ClientTimeout)) {
            Long timeoutConfig = (Long) configs.get(ConfigKey.ClientTimeout);

            return timeoutConfig == null ? Optional.empty() : Optional.of(timeoutConfig);
        } else {
            return Optional.empty();
        }
    }


    private ConnectionType getConnectionType(BeanSerializer serializer) {
        return (serializer instanceof JsonSerializer) ? ConnectionType.Json : ConnectionType.Common;

    }
}