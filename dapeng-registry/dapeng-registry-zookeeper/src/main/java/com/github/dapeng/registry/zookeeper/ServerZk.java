package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.api.lifecycle.LifecycleProcessorFactory;
import com.github.dapeng.core.helper.MasterHelper;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.lifecycle.LifeCycleEvent;
import com.github.dapeng.registry.RegistryAgent;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * 服务端  zk 服务注册
 *
 * @author hz.lei
 * @date 2018-03-20
 */
public class ServerZk extends CommonZk {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZk.class);

    private RegistryAgent registryAgent;

    /**
     * zk 配置 缓存 ，根据 serivceName + versionName 作为 key
     */
    public final Map<String, ZkServiceInfo> serviceInfoByName = new ConcurrentHashMap<>(128);

    public ServerZk(RegistryAgent registryAgent) {
        this.registryAgent = registryAgent;
    }

    /**
     * zk 客户端实例化
     * 使用 CountDownLatch 门闩 锁，保证zk连接成功后才返回
     */
    public synchronized void connect() {
        try {
            CountDownLatch semaphore = new CountDownLatch(1);
            // zk 需要为空
            destroy();

            zk = new ZooKeeper(zkHost, 30000, watchedEvent -> {
                LOGGER.warn("ServerZk::connect zkEvent:" + watchedEvent);
                switch (watchedEvent.getState()) {

                    case Expired:
                        //超时事件发生在Disconnected事件之后(如果断开连接后，sessionTimeout时间过了之后才连上zk服务端的话，就会产生Expired Event)
                        LOGGER.info("ServerZk session timeout to  {} [Zookeeper]", zkHost);
                        connect();
                        break;

                    case SyncConnected:
                        semaphore.countDown();
                        //创建根节点
                        createPersistNodeOnly(RUNTIME_PATH);
                        createPersistNodeOnly(CONFIG_PATH);
                        createPersistNodeOnly(ROUTES_PATH);

                        if (SoaSystemEnvProperties.SOA_FREQ_LIMIT_ENABLE) {
                            createPersistNodeOnly(FREQ_PATH);
                        }

                        LOGGER.info("ServerZk connected to  {} [Zookeeper]", zkHost);
                        if (registryAgent != null) {
                            registryAgent.registerAllServices();//重新注册服务
                        }

                        resyncZkInfos();
                        break;

                    case Disconnected:
                        //zookeeper重启或zookeeper实例重新创建
                        LOGGER.error("[Disconnected]: ServerZookeeper Registry zk 连接断开，可能是zookeeper重启或重建");

                        isMaster.clear(); //断开连接后，认为，master应该失效，避免某个孤岛一直以为自己是master
                        // 一般来说， zk重启导致的Disconnected事件不需要处理；
                        // 但对于zk实例重建，需要清理当前zk客户端并重连(自动重连的话会一直拿当前sessionId去重连).
                        connect();
                        break;
                    case AuthFailed:
                        LOGGER.info("Zookeeper connection auth failed ...");
                        destroy();
                        break;
                    default:
                        break;
                }
            });
            //hold 10 s
            semaphore.await(10000, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void resyncZkInfos() {
        synchronized (serviceInfoByName) {
            if (!serviceInfoByName.isEmpty()) {
                serviceInfoByName.values().forEach(serviceInfo -> {
                    syncZkConfigInfo(serviceInfo);
                    if (SoaSystemEnvProperties.SOA_FREQ_LIMIT_ENABLE) {
                        syncZkFreqControl(serviceInfo);
                    }
                });
            }
        }
    }

    /**
     * 关闭 zk 连接
     */
    public synchronized void destroy() {
        if (zk != null) {
            try {
                LOGGER.info("ServerZk closing connection to zookeeper {}", zkHost);
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin                                      ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～


    /**
     * 监听服务节点下面的子节点（临时节点，实例信息）变化
     */
    public void watchInstanceChange(RegisterContext context) {
        String watchPath = context.getServicePath();
        try {
            List<String> children = zk.getChildren(watchPath, event -> {
                LOGGER.warn("ServerZk::watchInstanceChange zkEvent:" + event);
                //Children发生变化，则重新获取最新的services列表
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("容器状态:{}, {}子节点发生变化，重新获取子节点...", ContainerFactory.getContainer().status(), event.getPath());
                    if (ContainerFactory.getContainer().status() == Container.STATUS_SHUTTING
                            || ContainerFactory.getContainer().status() == Container.STATUS_DOWN) {
                        LOGGER.warn("Container is shutting down");
                        return;
                    }
                    watchInstanceChange(context);
                }
            });
            boolean _isMaster = false;
            if (children.size() > 0) {
                _isMaster = checkIsMaster(children, MasterHelper.generateKey(context.getService(), context.getVersion()), context.getInstanceInfo());
            }
            //masterChange响应
            LifecycleProcessorFactory.getLifecycleProcessor().onLifecycleEvent(
                    new LifeCycleEvent(LifeCycleEvent.LifeCycleEventEnum.MASTER_CHANGE,
                            context.getService(), _isMaster));
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            registerRuntimeNode(context.getServicePath() + "/" + context.getInstanceInfo(), "", context);
        }
    }

    /**
     * 异步添加持久化节点回调方法
     */
    private AsyncCallback.StringCallback persistNodeCreateCallback = (rc, path, ctx, name) -> {
        LOGGER.warn("ServerZk::persistNodeCreateCallback zkEvent: " + rc + ", " + path + ", " + name);
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("创建节点:{},连接断开，重新创建", path);
                registerPersistNode(path, (String) ctx);
                break;
            case OK:
                LOGGER.info("创建节点:{},成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("创建节点:{},已存在", path);
                break;
            default:
                LOGGER.info("创建节点:{},失败", path);
        }
    };

    /**
     * 异步添加serverInfo 临时节点 的回调处理
     */
    private AsyncCallback.StringCallback serverRuntimeInfoCreateCallback = (rc, path, ctx, name) -> {
        LOGGER.warn("ServerZk::serverRuntimeInfoCreateCallback zkEvent: " + rc + ", " + path + ", " + name);
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("添加serviceInfo:{},连接断开，重新添加", path);
                //重新调用
                registerRuntimeNode(path, "", (RegisterContext) ctx);
                break;
            case OK:
                /**
                 * callback 时 注册监听
                 */
                watchInstanceChange((RegisterContext) ctx);

                LOGGER.info("添加serviceInfo:{},成功,注册实例监听watch watchInstanceChange", path);
                break;
            case NODEEXISTS:
                // 如果存在重复的临时节点， 删除之。
                // 由于目前临时节点采用CreateMode.EPHEMERAL_SEQUENTIAL的方式， 会自动带有一个序号(ip:port:version:seq)，
                // 故这个路径不可能存在
                LOGGER.error("添加serviceInfo:{},已存在!!", path);
                break;
            default:
                LOGGER.info("添加serviceInfo:{}，出错", path);
        }
    };

    public void setZookeeperHost(String zkHost) {
        this.zkHost = zkHost;
    }

    //-----竞选master---
    private static Map<String, Boolean> isMaster = MasterHelper.isMaster;

    private static final String CURRENT_CONTAINER_ADDR = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" +
            String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT);


    /**
     * 获取zk 配置信息，封装到 ZkConfigInfo
     * 加入并发考虑
     *
     * @param serviceName 服务名(服务唯一)
     * @return ZkServiceInfo
     */
    protected ZkServiceInfo getZkServiceInfo(String serviceName) {
        ZkServiceInfo info = serviceInfoByName.get(serviceName);
        if (info == null) {
            synchronized (serviceInfoByName) {
                info = serviceInfoByName.get(serviceName);
                if (info == null) {
                    info = new ZkServiceInfo(serviceName, new CopyOnWriteArrayList<>());
                    try {
                        // when container is shutdown, zk is down and will throw execptions
                        syncZkConfigInfo(info);
                        if (SoaSystemEnvProperties.SOA_FREQ_LIMIT_ENABLE) {
                            syncZkFreqControl(info);
                        }
                        serviceInfoByName.put(serviceName, info);
                    } catch (Throwable e) {
                        LOGGER.error("ServerZk::getConfigData failed." + e.getMessage());
                        info = null;
                    }
                }
            }
        }
        return info;
    }

    /**
     * 获取 zookeeper 上的 限流规则 freqRule
     *
     * @return
     */
    private void syncZkFreqControl(ZkServiceInfo serviceInfo) {
        if (zk == null || !zk.getState().isConnected()) {
            LOGGER.warn(getClass() + "::syncZkFreqControl zk is not ready, status:"
                    + (zk == null ? null : zk.getState()));
            return;
        }
        try {
            byte[] data = zk.getData(FREQ_PATH + "/" + serviceInfo.serviceName(), this, null);
            serviceInfo.freqControl(ZkDataProcessor.processFreqRuleData(serviceInfo.serviceName(), data));
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("获取freq 节点: {} 出现异常", serviceInfo.serviceName());
        }
    }

    @Override
    public void process(WatchedEvent event) {
        LOGGER.warn("ServerZk::process, zkEvent: " + event);
        if (event.getPath() == null) {
            // when zk restart, a zkEvent is trigger: WatchedEvent state:SyncConnected type:None path:null
            // we should ignore this.
            LOGGER.warn("ServerZk::process Just ignore this event.");
            return;
        }
        String serviceName = event.getPath().substring(event.getPath().lastIndexOf("/") + 1);
        ZkServiceInfo serviceInfo = serviceInfoByName.get(serviceName);
        if (serviceInfo == null) {
            LOGGER.warn("ServerZk::process, no such service: " + serviceName + " Just ignore this event.");
            return;
        }

        switch (event.getType()) {
            case NodeDataChanged:
                if (event.getPath().startsWith(CONFIG_PATH)) {
                    syncZkConfigInfo(serviceInfo);
                } else if (event.getPath().startsWith(FREQ_PATH)) {
                    syncZkFreqControl(serviceInfo);
                }
                break;
            default:
                LOGGER.warn("ClientZkAgent::process Just ignore this event.");
                break;
        }
    }

    public void registerRuntimeNode(String path, String data, RegisterContext context) {
        ZkUtils.createEphemeral(path, data, context, serverRuntimeInfoCreateCallback, zk);
    }

    public void registerPersistNode(String path, String data) {
        ZkUtils.createPersistent(path, data, persistNodeCreateCallback, zk);
    }

    /**
     * 仅创建持久节点， 不监听
     * @param path
     */
    public void createPersistNodeOnly(String path) {
        ZkUtils.createPersistNodeOnly(path, zk);
    }

    /**
     * @param children     当前方法下的实例列表，        eg 127.0.0.1:9081:1.0.0,192.168.1.12:9081:1.0.0
     * @param serviceKey   当前服务信息                eg com.github.user.UserService:1.0.0
     * @param instanceInfo 当前服务节点实例信息         eg  192.168.10.17:9081:1.0.0
     */
    public boolean checkIsMaster(List<String> children, String serviceKey, String instanceInfo) {
        if (children.size() <= 0) {
            return false;
        }

        boolean _isMaster = false;

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
            LOGGER.info("serviceInfo firstNode {}", firstNode);

            String firstInfo = firstNode.replace(firstNode.substring(firstNode.lastIndexOf(":")), "");

            if (firstInfo.equals(instanceInfo)) {
                isMaster.put(serviceKey, true);
                _isMaster = true;
                LOGGER.info("({})竞选master成功, master({})", serviceKey, CURRENT_CONTAINER_ADDR);
            } else {
                isMaster.put(serviceKey, false);
                _isMaster = false;
                LOGGER.info("({})竞选master失败，当前节点为({})", serviceKey);
            }
        } catch (NumberFormatException e) {
            LOGGER.error("临时节点格式不正确,请使用新版，正确格式为 etc. 192.168.100.1:9081:1.0.0:0000000022");
        }

        return _isMaster;
    }
}
