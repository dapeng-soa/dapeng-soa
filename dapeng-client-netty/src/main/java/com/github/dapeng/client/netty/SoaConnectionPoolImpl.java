package com.github.dapeng.client.netty;

import com.github.dapeng.cookie.CookieExecutor;
import com.github.dapeng.cookie.CookieRule;
import com.github.dapeng.core.*;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import com.github.dapeng.router.Route;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.zookeeper.LoadBalanceAlgorithm;
import com.github.dapeng.router.RoutesExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.github.dapeng.core.SoaCode.*;
import static com.github.dapeng.util.InvocationContextUtils.capsuleContext;

/**
 * @author lihuimin
 * @date 2017/12/22
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {
    private final Logger logger = LoggerFactory.getLogger(SoaConnectionPoolImpl.class);
    private final LoadBalanceStrategy DEFAULT_LB_STRATEGY = LoadBalanceStrategy.Random;

    private ClientRefManager clientRefManager = ClientRefManager.getInstance();

    static class ClientInfoSoftRef extends SoftReference<ClientInfo> {
        final ZkServiceInfo serviceInfo;
        final String serviceName;
        final String version;

        ClientInfoSoftRef(SoaConnectionPool.ClientInfo referent,
                          ZkServiceInfo zkServiceInfo,
                          ReferenceQueue<? super ClientInfo> q) {
            super(referent, q);
            this.serviceName = referent.serviceName;
            this.version = referent.version;
            this.serviceInfo = zkServiceInfo;
        }
    }

    public SoaConnectionPoolImpl() {
        IdleConnectionManager connectionManager = new IdleConnectionManager();
        connectionManager.start();
    }

    @Override
    public ClientInfo registerClientInfo(String serviceName, String version) {
        return clientRefManager.registerClient(serviceName, version);
    }

    @Override
    public <REQ, RESP> RESP send(
            String service, String version,
            String method, REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer)
            throws SoaException {
        ZkServiceInfo serviceInfo = clientRefManager.serviceInfo(service);
        if (serviceInfo == null) {
            logger.error(getClass() + "::send serviceInfo not found: " + service);
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }

        SoaConnection connection = retryFindConnection(serviceInfo, version, method);
        // 选好的服务版本(可能不同于请求的版本)
        String serverVersion = InvocationContextImpl.Factory.currentInstance().versionName();
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }
        long timeout = getTimeout(serviceInfo, method);
        if (logger.isDebugEnabled()) {
            logger.debug("findConnection:serviceName:{},methodName:{},version:[{} -> {}] ,TimeOut:{}",
                    service, method, version, serverVersion, timeout);
        }
        return connection.send(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(
            String service, String version,
            String method, REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer) throws SoaException {
        ZkServiceInfo serviceInfo = clientRefManager.serviceInfo(service);
        if (serviceInfo == null) {
            logger.error(getClass() + "::sendAsync serviceInfo not found: " + service);
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }

        SoaConnection connection = retryFindConnection(serviceInfo, version, method);

        String serverVersion = InvocationContextImpl.Factory.currentInstance().versionName();
        if (connection == null) {
            throw new SoaException(SoaCode.NotFoundServer, "服务 [ " + service + " ] 无可用实例");
        }
        long timeout = getTimeout(serviceInfo, method);
        if (logger.isDebugEnabled()) {
            logger.debug("findConnection:serviceName:{},methodName:{},version:[{} -> {}] ,TimeOut:{}",
                    service, method, version, serverVersion, timeout);
        }
        return connection.sendAsync(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    @Override
    public RuntimeInstance getRuntimeInstance(String service, String serviceIp, int servicePort) {
        return clientRefManager.serviceInfo(service).runtimeInstance(serviceIp, servicePort);
    }

    private SoaConnection findConnection(final ZkServiceInfo serviceInfo,
                                         final String version,
                                         final String method) throws SoaException {

        InvocationContextImpl context = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

        //设置慢服务检测时间阈值
        /*Optional<Long> maxProcessTime = getZkProcessTime(method, zkInfo);
        context.maxProcessTime(maxProcessTime.orElse(null));*/
        // TODO: 2018-10-12 慢服务时间 取自超时时间[TimeOut]
        context.maxProcessTime(getTimeout(serviceInfo, method));

        //如果设置了calleeip 和 calleport 直接调用服务 不走路由
        if (context.calleeIp().isPresent() && context.calleePort().isPresent()) {
            return SubPoolFactory.getSubPool(IPUtils.transferIp(context.calleeIp().get()), context.calleePort().get()).getConnection();
        }

        //当zk上服务节点发生变化的时候, 可能会导致拿到不存在的服务运行时实例或者根本拿不到任何实例.
        List<RuntimeInstance> compatibles = serviceInfo.runtimeInstances();
        if (compatibles == null || compatibles.isEmpty()) {
            return null;
        }

        // checkVersion
        List<RuntimeInstance> checkVersionInstances = new ArrayList<>(8);
        for (RuntimeInstance rt : compatibles) {
            if (checkVersion(version, rt.version)) {
                checkVersionInstances.add(rt);
            }
        }

        if (checkVersionInstances.isEmpty()) {
            logger.error(getClass().getSimpleName() + "::findConnection[service: " + serviceInfo.serviceName() + ":" + version + "], not found available version of instances");
            throw new SoaException(NoMatchedService, "服务 [ " + serviceInfo.serviceName() + ":" + version + "] 无可用实例:没有找到对应的服务版本");
        }
        // router
        // 把路由需要用到的条件放到InvocationContext中
        capsuleContext(context, serviceInfo.serviceName(), version, method);

        List<RuntimeInstance> routedInstances = router(serviceInfo, checkVersionInstances);

        if (routedInstances == null || routedInstances.isEmpty()) {
            logger.error(getClass().getSimpleName() + "::findConnection[service: " + serviceInfo.serviceName() + "], not found available instances by routing rules");
            throw new SoaException(NoMatchedRouting, "服务 [ " + serviceInfo.serviceName() + " ] 无可用实例:路由规则没有解析到可运行的实例");
        }

        //loadBalance
        RuntimeInstance inst = loadBalance(method, serviceInfo, routedInstances);
        if (inst == null) {
            // should not reach here
            throw new SoaException(NotFoundServer, "服务 [ " + serviceInfo.serviceName() + " ] 无可用实例:负载均衡没有找到合适的运行实例");
        }

        inst.increaseActiveCount();

        // TODO: 2018-08-04  服务端需要返回来正确的版本号
        context.versionName(inst.version);

        return SubPoolFactory.getSubPool(inst.ip, inst.port).

                getConnection();

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
     * 服务路由
     *
     * @param serviceInfo
     * @param compatibles
     * @return
     */
    private List<RuntimeInstance> router(ZkServiceInfo serviceInfo, List<RuntimeInstance> compatibles) throws SoaException {
        List<Route> routes = serviceInfo.routes();
        if (routes == null || routes.size() == 0) {
            logger.debug("router 获取 路由信息为空或size为0,跳过router,服务实例数：{}", compatibles.size());
            return compatibles;
        } else {
            InvocationContextImpl context = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

            List<RuntimeInstance> runtimeInstances = RoutesExecutor.executeRoutes(context, routes, compatibles);
            if (runtimeInstances.size() == 0) {
                throw new SoaException(SoaCode.NoMatchedRouting);
            }
            return runtimeInstances;
        }
    }

    /**
     * 如果出现异常（ConcurrentModifyException）,或获取到的实例为0，进行重试
     */
    private SoaConnection retryFindConnection(final ZkServiceInfo serviceInfo,
                                              final String version,
                                              final String method) throws SoaException {
        SoaConnection soaConnection;
        int retry = 1;
        do {
            try {
                soaConnection = findConnection(serviceInfo, version, method);
                if (soaConnection != null) {
                    return soaConnection;
                }
            } catch (SoaException e) {
                throw e;
            } catch (Exception e) {
                logger.error("zkInfo get connection 出现异常: " + e.getMessage(),e);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        } while (retry++ <= 3);
        logger.warn("retryFindConnection::重试3次获取 connection 失败");
        return null;
    }

    /**
     * 根据zk 负载均衡配置解析，分为 全局/service级别/method级别
     *
     * @param methodName
     * @param zkServiceInfo
     * @param compatibles
     * @return
     */
    private RuntimeInstance loadBalance(String methodName, ZkServiceInfo zkServiceInfo, List<RuntimeInstance> compatibles) {

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
     * 2. invocationContext没有的话, 就拿ZK设置的
     * 3. ZK没有设置的话, 那么取Option的(命令行或者环境变量)
     * 4. 没设置Option的话, 拿IDL的(暂没实现该参数)
     * 5. 都没有的话, 拿默认值.(这个值所有方法一致, 假设为50S)
     * <p>
     * 最后校验一下,拿到的值不能超过系统设置的最大值
     *
     * @param method
     * @param serviceInfo
     * @return
     */
    private long getTimeout(ZkServiceInfo serviceInfo, String method) {

        long maxTimeout = SoaSystemEnvProperties.SOA_MAX_TIMEOUT;
        long defaultTimeout = SoaSystemEnvProperties.SOA_DEFAULT_TIMEOUT;

        Optional<Integer> invocationTimeout = getInvocationTimeout();
        Optional<Long> envTimeout = SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT == 0 ?
                Optional.empty() : Optional.of(SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT);

        Optional<Long> zkTimeout = getZkTimeout(method, serviceInfo);
        Optional<Long> idlTimeout = getIdlTimeout(method);

        Optional<Long> timeout;
        if (invocationTimeout.isPresent()) {
            timeout = invocationTimeout.map(Long::valueOf);
        } else if (zkTimeout.isPresent()) {
            timeout = zkTimeout;
        } else if (envTimeout.isPresent()) {
            timeout = envTimeout;
        } else if (idlTimeout.isPresent()) {
            timeout = idlTimeout;
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
    private Optional<Long> getIdlTimeout(String methodName) {
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
    private Optional<Long> getZkTimeout(String methodName, ZkServiceInfo configInfo) {
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