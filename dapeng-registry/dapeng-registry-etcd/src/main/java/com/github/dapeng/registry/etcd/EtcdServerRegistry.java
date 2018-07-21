package com.github.dapeng.registry.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.options.PutOption;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.helper.MasterHelper;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.RegisterContext;
import com.github.dapeng.registry.RegisterInfo;
import com.github.dapeng.registry.RegistryServerAgent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * desc: EtcdServerRegistry
 *
 * @author hz.lei
 * @since 2018年07月19日 下午4:50
 */
public class EtcdServerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(EtcdServerRegistry.class);

    private RegistryServerAgent registryAgent;

    private String etcdRegisterHost = SoaSystemEnvProperties.SOA_ETCD_HOST;

    private final static String SERVICE_PATH = "/soa/runtime/services";
    private final static String CONFIG_PATH = "/soa/config/services";
    private final static String ROUTES_PATH = "/soa/config/routes";


    /**
     * zk 配置 缓存 ，根据 serivceName + versionName 作为 key
     */
    public final ConcurrentMap<String, RegisterInfo> zkConfigMap = new ConcurrentHashMap();

    public EtcdServerRegistry(RegistryServerAgent registryAgent) {
        this.registryAgent = registryAgent;
    }

    public void setEtcdRegisterHost(String etcdRegisterHost) {
        this.etcdRegisterHost = etcdRegisterHost;
    }

    private Lease lease;
    private KV kv;
    private long leaseId;
    private Client client;

    /**
     * etcd 客户端实例化,并保持与etcd的心跳
     */
    public void connect() {
        client = Client.builder().endpoints(etcdRegisterHost).build();
        this.lease = client.getLeaseClient();
        this.kv = client.getKVClient();
        try {
            this.leaseId = lease.grant(10).get().getID();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        keepAlive();
    }

    /**
     * 发送心跳到ETCD,表明该host是活着的
     */
    private void keepAlive() {
        //构建定时线程池
        ExecutorService executorService = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("dapeng-etcd-keep-alive-%d")
                        .build());
        //保持心跳
        executorService.submit(
                () -> {
                    try {
                        Lease.KeepAliveListener listener = lease.keepAlive(leaseId);
                        listener.listen();
                        logger.info("KeepAlive lease:" + leaseId + "; Hex format:" + Long.toHexString(leaseId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }


    /*public void register(String serviceName, int port) throws Exception {
        // 服务注册的key为:    /dubbomesh/com.some.package.IHelloService/192.168.100.100:2000
        String strKey = MessageFormat.format("/{0}/{1}/{2}:{3}", rootPath, serviceName, IpHelper.getHostIp(), String.valueOf(port));
        ByteSequence key = ByteSequence.fromString(strKey);
        String weight = System.getProperty("lb.weight");
        ByteSequence val;
        if (StringUtils.isEmpty(weight)) {
            weight = "1";
            val = ByteSequence.fromString(weight);
            logger.warn("未设置provider权重，默认设置为1");
        } else {
            val = ByteSequence.fromString(weight);
        }
        kv.put(key, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        kv.txn();
        logger.info("Register a new service at:{},weight:{}", strKey, weight);
    }*/


    /**
     * 关闭 etcd 连接
     */
    public void destroy() {
        if (client != null) {
            logger.info("ServerZk closing connection to zookeeper {}", etcdRegisterHost);
            client.close();
            client = null;
        }
    }


    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin                                      ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～


    /**
     * 节点创建
     */
    public void register(String servicePath, String instancePath) {
        ByteSequence serviceKey = ByteSequence.fromString(servicePath);
        ByteSequence instanceKey = ByteSequence.fromString(instancePath);
        ByteSequence val = ByteSequence.fromString("");

        kv.put(serviceKey, val);
        kv.txn();

        if (instancePath != null) {
            PutResponse putResponse = null;
            try {
                putResponse = kv.put(instanceKey, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
                kv.txn();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("etcd 创建节点出错:" + e.getMessage(), e);
            }
            logger.info("节点 {} 创建成功,返回内容: {}\n", instanceKey, putResponse.toString());

        }
    }


    /**
     * 监听服务节点下面的子节点（临时节点，实例信息）变化
     */
    public void watchInstChange(RegisterContext context) {
        //Children发生变化，则重新获取最新的services列表
        logger.info("容器状态:{}, {}子节点发生变化，重新获取子节点...", ContainerFactory.getContainer().status());

        if (ContainerFactory.getContainer().status() == Container.STATUS_SHUTTING
                || ContainerFactory.getContainer().status() == Container.STATUS_DOWN) {
            logger.warn("Container is shutting down");
            return;
        }
        List<String> children = new ArrayList<>();
        EtcdUtils.etcdWatch(client.getWatchClient(), context.getServicePath(), true, events -> {
            events.forEach(event -> {
                String value = event.getKeyValue().getValue().toStringUtf8();
                children.add(value);
            });
        });
        if (children.size() > 0) {
            checkIsMaster(children.stream().map(EtcdUtils::getInstData).collect(Collectors.toList()),
                    MasterHelper.generateKey(context.getService(), context.getVersion()), context.getInstanceInfo());
        }
    }


    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    private void createPersistent(String path, String data) {
        Stat stat = exists(path);

        if (stat == null) {
            zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, persistNodeCreateCallback, data);
        }
    }

    private Stat exists(String path) {
        Stat stat = null;
        try {
            stat = zk.exists(path, false);
        } catch (KeeperException | InterruptedException e) {
        }
        return stat;
    }


    //-----竞选master---
    private static Map<String, Boolean> isMaster = MasterHelper.isMaster;

    /**
     * @param children     当前方法下的实例列表，        eg 127.0.0.1:9081:1.0.0,192.168.1.12:9081:1.0.0
     * @param serviceKey   当前服务信息                eg com.github.user.UserService:1.0.0
     * @param instanceInfo 当前服务节点实例信息         eg  192.168.10.17:9081:1.0.0
     */
    public void checkIsMaster(List<String> children, String serviceKey, String instanceInfo) {
        if (children.size() <= 0) {
            return;
        }
        /**
         * 排序规则
         * a: 192.168.100.1:9081:1.0.0:0000000022
         * b: 192.168.100.1:9081:1.0.0:0000000014
         * 根据 lastIndexOf :  之后的数字进行排序，由小到大，每次取zk临时有序节点中的序列最小的节点作为master
         */
        try {
            Collections.sort(children, (o1, o2) -> {
                Integer int1 = Integer.valueOf(o1.substring(o1.lastIndexOf(":") + 1));
                Integer int2 = Integer.valueOf(o2.substring(o2.lastIndexOf(":") + 1));
                return int1 - int2;
            });

            String firstNode = children.get(0);
            logger.info("serviceInfo firstNode {}", firstNode);

            String firstInfo = firstNode.replace(firstNode.substring(firstNode.lastIndexOf(":")), "");

            if (firstInfo.equals(instanceInfo)) {
                isMaster.put(serviceKey, true);
                logger.info("({})竞选master成功, master({})", serviceKey, CURRENT_CONTAINER_ADDR);
            } else {
                isMaster.put(serviceKey, false);
                logger.info("({})竞选master失败，当前节点为({})", serviceKey);
            }
        } catch (NumberFormatException e) {
            logger.error("临时节点格式不正确,请使用新版，正确格式为 etc. 192.168.100.1:9081:1.0.0:0000000022");
        }
    }


    private static final String CURRENT_CONTAINER_ADDR = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" +
            String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT);


    /**
     * 获取zk 配置信息，封装到 ZkConfigInfo
     *
     * @param serviceName
     * @return
     */
    protected RegisterInfo getConfigData(String serviceName) {

        RegisterInfo info = zkConfigMap.get(serviceName);
        if (info != null) {
            return info;
        }
        info = new RegisterInfo(serviceName);
        syncZkConfigInfo(info);
        zkConfigMap.put(serviceName, info);
        return info;
    }

    /**
     * sync service
     *
     * @param zkInfo
     */
    private void syncZkConfigInfo(RegisterInfo zkInfo) {
        try {
            String configPath = MessageFormat.format("{0}/{1}", CONFIG_PATH, zkInfo.service);

            EtcdUtils.etcdWatch(client.getWatchClient(), configPath, false, events -> {
                //
                events.forEach(event -> {
                    KeyValue kv = event.getKeyValue();

                });
            });
            String data = getEtcdValue(configPath, false);
            EtcdUtils.processEtcdConfig(data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String getEtcdValue(String path, Boolean usePrefix) {
        try {
            KV kv = client.getKVClient();
            ByteSequence seqKey = ByteSequence.fromString(path);
            GetResponse response = kv.get(seqKey).get();
            String key = response.getKvs().get(0).getKey().toStringUtf8();
            String value = response.getKvs().get(0).getValue().toStringUtf8();
            logger.info("Get data from etcdServer, key:{}, value:{}", key, value);
            return value;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
