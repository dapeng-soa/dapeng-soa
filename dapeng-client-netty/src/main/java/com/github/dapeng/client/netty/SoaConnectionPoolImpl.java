package com.github.dapeng.client.netty;

import com.github.dapeng.core.*;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.registry.*;
import com.github.dapeng.registry.zookeeper.LoadBalanceService;
import com.github.dapeng.registry.zookeeper.ZkClientAgentImpl;
import com.github.dapeng.util.SoaSystemEnvProperties;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private Map<String, ServiceZKInfo> zkInfos = new ConcurrentHashMap<>();
    private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();
    private ZkClientAgent zkAgent = new ZkClientAgentImpl();

    private ReentrantLock subPoolLock = new ReentrantLock();

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
        // TODO
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
            throw new SoaException(SoaCode.NotConnected);
        }

        return connection.send(service, version, method, request, requestSerializer, responseSerializer);
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service,
                                              String version,
                                              String method,
                                              REQ request,
                                              BeanSerializer<REQ> requestSerializer,
                                              BeanSerializer<RESP> responseSerializer,
                                              long timeout) throws SoaException {

        ConnectionType connectionType = getConnectionType(requestSerializer);
        SoaConnection connection = findConnection(service, version,
                method, connectionType);
        if (connection == null) {
            throw new SoaException(SoaCode.NotConnected);
        }
        return connection.sendAsync(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    private SoaConnection findConnection(String service,
                                         String version,
                                         String method,
                                         ConnectionType connectionType) {
        ServiceZKInfo zkInfo = zkInfos.get(service);

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


    private ConnectionType getConnectionType(BeanSerializer serializer) {
        return (serializer instanceof JsonSerializer) ? ConnectionType.Json : ConnectionType.Common;

    }
}