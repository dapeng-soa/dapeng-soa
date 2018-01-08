package com.github.dapeng.registry.zookeeper;


import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
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

/**
 * Created by lihuimin on 2017/12/24.
 */
public class ZkClientAgentImpl implements ZkClientAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkClientAgentImpl.class);

    private ZookeeperWatcher siw, zkfbw;

    public ZkClientAgentImpl(){
        start();
    }

    @Override
    public void start() {

        siw = new ZookeeperWatcher(true, SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST);
        siw.init();

        if (SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG) {
            zkfbw = new ZookeeperWatcher(true, SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_HOST);
            zkfbw.init();
        }

    }

    @Override
    public void stop() {
        if (siw != null) {
            siw.destroy();
        }

        if (zkfbw != null) {
            zkfbw.destroy();
        }

    }

    @Override
    public void cancnelSyncService(String service) {


    }

    @Override
    public void syncService(String serviceName, Map<String,ServiceZKInfo> zkInfos) {

        boolean usingFallbackZookeeper = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG;

        ServiceZKInfo zkInfo = zkInfos.get(serviceName);
        if(zkInfo == null){  //zkInfos没有，从zookeeper拿
            zkInfo = siw.getServiceZkInfo(serviceName,zkInfos);
            if(zkInfo == null && usingFallbackZookeeper){
                zkInfo = zkfbw.getServiceZkInfo(serviceName,zkInfos);
            }
        }

        //使用路由规则，过滤可用服务器
        InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();
        List<Route> routes = usingFallbackZookeeper ? zkfbw.getRoutes() : siw.getRoutes();
        List<RuntimeInstance> runtimeList = new ArrayList<>();

        if (zkInfo != null&&zkInfo.getRuntimeInstances()!=null) {
            for (RuntimeInstance instance : zkInfo.getRuntimeInstances()) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(instance.ip);
                    if (RouteExecutor.isServerMatched(context, routes, inetAddress)) {
                        runtimeList.add(instance);
                    }
                } catch (UnknownHostException e) {
                    LOGGER.error(e.getMessage(),e);
                }
            }
            zkInfo.setRuntimeInstances(runtimeList);
            zkInfos.put(serviceName,zkInfo);
        }

    }


    @Override
    public Map<ConfigKey, Object> getConfig(boolean usingFallback, String serviceKey) {

        if (usingFallback) {
            if (zkfbw.getConfigWithKey(serviceKey).entrySet().size() <= 0) {
                return null;
            } else {
                return zkfbw.getConfigWithKey(serviceKey);
            }
        } else {

            if (siw.getConfigWithKey(serviceKey).entrySet().size() <= 0) {
                return null;
            } else {
                return siw.getConfigWithKey(serviceKey);
            }
        }
    }
}
