package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.version.Version;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * zookeeperClient 主要作用是 供 客户端 从zk获取服务实例
 *
 * @author tangliu
 * @date 2016/2/29
 */
public class ClientZk extends CommonZk {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientZk.class);

    private final Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();
    /**
     * 其他配置信息
     */
    private final Map<String, Map<ConfigKey, Object>> config = new ConcurrentHashMap<>();
    /**
     * 路由配置信息
     */
    private final Map<String, List<Route>> routesMap = new ConcurrentHashMap<>(16);


    public ClientZk(String zkHost) {
        this.zkHost = zkHost;
    }

    public void init() {
        connect();
    }

    /**
     * 连接zookeeper
     */
    private void connect() {
        try {
            CountDownLatch semaphore = new CountDownLatch(1);

            // Fixme zk连接状态变化不应该引起本地runtime缓存的清除, 尤其是zk挂了之后, 不至于影响业务(本地缓存还存在于每个SoaConnectionPool中?)
            zk = new ZooKeeper(zkHost, 30000, e -> {
                switch (e.getState()) {
                    case Expired:
                        LOGGER.info("Client's host: {} 到zookeeper Server的session过期，重连", zkHost);

                        destroy();
                        init();
                        break;
                    case SyncConnected:
                        semaphore.countDown();
                        LOGGER.info("Client's host: {}  已连接 zookeeper Server", zkHost);
                        caches.clear();
                        config.clear();
                        break;
                    case Disconnected:
                        LOGGER.error("Client's host: {} 到zookeeper的连接被断开", zkHost);
                        destroy();
                        init();
                        break;
                    case AuthFailed:
                        LOGGER.info("Zookeeper connection auth failed ...");
                        destroy();
                        break;
                    default:
                        break;
                }
            });
            semaphore.await(10000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
    }

    public void destroy() {
        if (zk != null) {
            try {
                LOGGER.info("Client's host: {} 关闭到zookeeper的连接", zkHost);
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }


    /**
     * route 根据给定路由规则对可运行实例进行过滤
     *
     * @return
     */
    public synchronized List<Route> getRoutes(String service) {
        if (routesMap.get(service) == null) {
            try {
                byte[] data = zk.getData(ROUTES_PATH, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                        LOGGER.info("routes 节点 data 发现变更，重新获取信息");
                        routesMap.clear(); // fixme
                        getRoutes(service);
                    }
                }, null);
                processRouteData(service, data);
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("get route data failed ");
            }
        }
        return this.routesMap.get(service);
    }

    /**
     * process zk data 解析route 信息
     */
    public void processRouteData(String service, byte[] data) {
        try {
            String routeData = new String(data, "utf-8");
            List<Route> zkRoutes = RoutesExecutor.parseAll(routeData);
            routesMap.put(service, zkRoutes);
        } catch (Exception e) {
            LOGGER.error("parser routes 信息 失败，请检查路由规则写法是否正确!");
        }
    }

    /**
     * 客户端 同步zk 服务信息  syncServiceZkInfo
     *
     * @param zkInfo
     */
    public void syncServiceZkInfo(ZkServiceInfo zkInfo) {
        try {
            // sync runtimeList
            syncZkRuntimeInfo(zkInfo);
            //syncService config
            syncZkConfigInfo(zkInfo);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        //判断,runtimeList size
        if (zkInfo.getRuntimeInstances().size() > 0) {
            zkInfo.setStatus(ZkServiceInfo.Status.ACTIVE);
        }
    }

    /**
     * 保证zk watch机制，出现异常循环执行5次
     *
     * @param zkInfo
     */
    private void syncZkRuntimeInfo(ZkServiceInfo zkInfo) {
        String servicePath = SERVICE_PATH + "/" + zkInfo.service;
        int retry = 5;
        do {
            try {
                if (zk == null) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:zk is null, now init()");
                    init();
                }

                List<String> childrens;
                try {
                    childrens = zk.getChildren(servicePath, watchedEvent -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                                LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                                syncZkRuntimeInfo(zkInfo);
                            }
                        }
                    });
                } catch (KeeperException.NoNodeException e) {
                    LOGGER.error("sync service:  {} zk node is not exist,", zkInfo.service);
                    return;
                }

                if (childrens.size() == 0) {
                    zkInfo.setStatus(ZkServiceInfo.Status.CANCELED);
                    zkInfo.getRuntimeInstances().clear();
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:no service instances found");
                    return;
                }
                List<RuntimeInstance> runtimeInstanceList = zkInfo.getRuntimeInstances();
                //这里要clear掉，因为接下来会重新将实例信息放入list中，不清理会导致重复...
                runtimeInstanceList.clear();
                LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "], 获取{}的子节点成功", servicePath);
                //child = 10.168.13.96:9085:1.0.0
                for (String children : childrens) {
                    String[] infos = children.split(":");
                    RuntimeInstance instance = new RuntimeInstance(zkInfo.service, infos[0], Integer.valueOf(infos[1]), infos[2]);
                    runtimeInstanceList.add(instance);
                }

                StringBuilder logBuffer = new StringBuilder();
                zkInfo.getRuntimeInstances().forEach(info -> logBuffer.append(info.toString()));
                LOGGER.info("<-> syncZkRuntimeInfo 触发服务实例同步，目前服务实例列表:" + zkInfo.service + " -> " + logBuffer);
                zkInfo.setStatus(ZkServiceInfo.Status.ACTIVE);
                return;
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        } while (retry-- > 0);
    }


    private Watcher runtimeWatcher(ZkServiceInfo zkInfo) {
        return watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    syncZkRuntimeInfo(zkInfo);
                }
            }
        };
    }


    //～～～～～～～～～～～～～～～～～～
    //      目前暂时没有用到的方法
    //～～～～～～～～～～～～～～～

    public List<ServiceInfo> getServiceInfoCached(String serviceName, String versionName, boolean compatible) {

        List<ServiceInfo> serverList;

        if (caches.containsKey(serviceName)) {
            serverList = caches.get(serviceName);
        } else {
            //get service Info and set up Watcher
            getServiceInfoByServiceName(serviceName);
            serverList = caches.get(serviceName);
        }

        List<ServiceInfo> usableList = new ArrayList<>();

        if (serverList != null && serverList.size() > 0) {

            if (!compatible) {
                usableList.addAll(serverList.stream().filter(server -> server.versionName.equals(versionName)).collect(Collectors.toList()));
            } else {
                usableList.addAll(serverList.stream().filter(server -> Version.toVersion(versionName).compatibleTo(Version.toVersion(server.versionName))).collect(Collectors.toList()));
            }
        }
        return usableList;
    }

    /**
     * 根据serviceName节点的路径，获取下面的子节点，并监听子节点变化
     *
     * @param serviceName
     */
    private void getServiceInfoByServiceName(String serviceName) {

        String servicePath = SERVICE_PATH + "/" + serviceName;
        try {
            if (zk == null) {
                init();
            }

            List<String> children = zk.getChildren(servicePath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    getServiceInfoByServiceName(serviceName);
                }
            });

            LOGGER.info("获取{}的子节点成功", servicePath);
            WatcherUtils.resetServiceInfoByName(serviceName, servicePath, children, caches);

        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /*

    public void getRoutesAsync() {
        zk.getData(ROUTES_PATH, event -> {
            if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                LOGGER.info("routes 节点 data 发现变更，重新获取信息");
                routes.clear();
                getRoutesAsync();
            }
        }, routeDataCb, null);
    }

    private AsyncCallback.DataCallback routeDataCb = (rc, path, ctx, data, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                getRoutesAsync();
                break;
            case NONODE:
                LOGGER.error("服务 [{}] 的service配置节点不存在，无法获取service级配置信息 ", ((ZkServiceInfo) ctx).service);
                break;
            case OK:
                processRouteData(data);
                break;
            default:
                break;
        }
    };

    */
}
