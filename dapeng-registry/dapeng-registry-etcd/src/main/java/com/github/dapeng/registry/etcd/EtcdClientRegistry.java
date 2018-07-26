package com.github.dapeng.registry.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.RegisterInfo;
import com.github.dapeng.registry.RegisterUtils;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * desc: ClientRegistry
 *
 * @author hz.lei
 * @since 2018年07月26日 下午5:54
 */
public class EtcdClientRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClientRegistry.class);

    private final Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();


    private final static String SERVICE_PATH = "/soa/runtime/services";
    private final static String CONFIG_PATH = "/soa/config/services";
    private final static String ROUTES_PATH = "/soa/config/routes";


    /**
     * 其他配置信息
     */
    private final Map<String, Map<ConfigKey, Object>> config = new ConcurrentHashMap<>();
    /**
     * 路由配置信息
     */
    private final Map<String, List<Route>> routesMap = new ConcurrentHashMap<>(16);

    private String etcdServerAddress;

    private Lease lease;
    private KV kv;
    private Client client;


    public EtcdClientRegistry(String etcdServerAddress) {
        this.etcdServerAddress = etcdServerAddress;
    }

    public void init() {
        connect();
    }

    /**
     * 连接 etcd
     */
    /**
     * etcd 客户端实例化,并保持与etcd的心跳
     */
    private void connect() {
        client = Client.builder().endpoints(etcdServerAddress).build();
        this.lease = client.getLeaseClient();
        this.kv = client.getKVClient();
    }

    public void destroy() {

    }


    /**
     * get data by key
     */
    public List<Route> getRoutes(Client client, String servicePath) {
        if (routesMap.get(servicePath) == null) {
            try {
                GetResponse response = client.getKVClient().get(ByteSequence.fromString(servicePath)).get();
                String routeData = response.getKvs().get(0).getValue().toStringUtf8();
                List<Route> routes = processRouteData(servicePath, routeData);

                EtcdUtils.etcdWatch(client.getWatchClient(), servicePath, Boolean.FALSE, events -> {
                    getRoutes(client, servicePath);
                });
                LOGGER.warn("ClientZk::getRoutes routes changes:" + routes);
                return routes;
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error("获取route service 节点: {} 出现异常", servicePath);
            }
        } else {
            LOGGER.debug("获取route信息, service: {} , route size {}", servicePath, routesMap.get(servicePath).size());
            return this.routesMap.get(servicePath);
        }
        return null;
    }


    /**
     * process zk data 解析route 信息
     */
    private List<Route> processRouteData(String service, String data) {
        List<Route> zkRoutes;
        try {
            zkRoutes = RoutesExecutor.parseAll(data);
            routesMap.put(service, zkRoutes);
        } catch (Exception e) {
            zkRoutes = new ArrayList<>(16);
            LOGGER.error("parser routes 信息 失败，请检查路由规则写法是否正确!");
        }
        return zkRoutes;
    }

    /**
     * 客户端 同步zk 服务信息  syncServiceZkInfo
     *
     * @param zkInfo
     */
    public void syncServiceInfo(RegisterInfo zkInfo) {
        try {
            // sync runtimeList
            syncEtcdRuntimeService(zkInfo);
            //syncService config
            syncEtcdConfigService(zkInfo);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        //判断,runtimeList size
        if (zkInfo.getRuntimeInstances().size() > 0) {
            zkInfo.setStatus(RegisterInfo.Status.ACTIVE);
        }
    }

    /**
     * 保证zk watch机制，出现异常循环执行5次
     *
     * @param zkInfo
     */
    private void syncEtcdRuntimeService(RegisterInfo zkInfo) {
        String servicePath = SERVICE_PATH + "/" + zkInfo.service;
        if (client == null) {
            LOGGER.info(getClass().getSimpleName() + "::syncEtcdRuntimeService[" + zkInfo.service + "]:zk is null, now init()");
            init();
        }
        List<String> children;
        ByteSequence byteSeq = ByteSequence.fromString(servicePath);

        try {
            GetResponse response = client.getKVClient().get(byteSeq, GetOption.newBuilder().withPrefix(byteSeq).build()).get();
            children = response.getKvs().stream()
                    .map(KeyValue::getValue)
                    .map(ByteSequence::toStringUtf8)
                    .collect(Collectors.toList());


            EtcdUtils.etcdWatch(client.getWatchClient(), servicePath, Boolean.TRUE, events -> {
                syncEtcdRuntimeService(zkInfo);
            });


            if (children.size() == 0) {
                zkInfo.setStatus(RegisterInfo.Status.CANCELED);
                zkInfo.getRuntimeInstances().clear();
                LOGGER.info(getClass().getSimpleName() + "::syncEtcdRuntimeService[" + zkInfo.service + "]:no service instances found");
            }

            List<RuntimeInstance> runtimeInstanceList = zkInfo.getRuntimeInstances();
            //这里要clear掉，因为接下来会重新将实例信息放入list中，不清理会导致重复...
            runtimeInstanceList.clear();
            LOGGER.info(getClass().getSimpleName() + "::syncEtcdRuntimeService[" + zkInfo.service + "], 获取{}的子节点成功", servicePath);
            //child = 10.168.13.96:9085:1.0.0
            for (String child : children) {
                String[] infos = child.split(":");
                RuntimeInstance instance = new RuntimeInstance(zkInfo.service, infos[0], Integer.valueOf(infos[1]), infos[2]);
                runtimeInstanceList.add(instance);
            }

            StringBuilder logBuffer = new StringBuilder();
            zkInfo.getRuntimeInstances().forEach(info -> logBuffer.append(info.toString()));
            LOGGER.info("<-> syncEtcdRuntimeService 触发服务实例同步，目前服务实例列表:" + zkInfo.service + " -> " + logBuffer);
            zkInfo.setStatus(RegisterInfo.Status.ACTIVE);
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("sync service:  {} etcd key is not exist,", zkInfo.service);
        }
    }

    /**
     * sync etcd config key /value
     *
     * @param zkInfo
     */
    protected void syncEtcdConfigService(RegisterInfo zkInfo) throws ExecutionException, InterruptedException {
        //1.获取 globalConfig  异步模式
        String servicePath = SERVICE_PATH + "/" + zkInfo.service;


        if (client == null) {
            LOGGER.info(getClass().getSimpleName() + "::syncEtcdRuntimeService[" + zkInfo.service + "]:zk is null, now init()");
            init();
        }

        List<String> children;
        ByteSequence byteSeq = ByteSequence.fromString(CONFIG_PATH);

        GetResponse response = client.getKVClient().get(byteSeq).get();
        String globalData = response.getKvs().get(0).getValue().toStringUtf8();
        RegisterUtils.processZkConfig(globalData, zkInfo, true);

        EtcdUtils.etcdWatch(client.getWatchClient(), servicePath, Boolean.TRUE, events -> {
            syncEtcdRuntimeService(zkInfo);
        });


    }


}
