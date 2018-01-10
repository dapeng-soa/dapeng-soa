package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.version.Version;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.RuntimeInstance;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.registry.ServiceZKInfo;
import com.github.dapeng.route.Route;
import com.github.dapeng.route.parse.RouteParser;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by tangliu on 2016/2/29.
 */
public class ZookeeperWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperWatcher.class);

    private final boolean isClient;
    private final Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();
    private final Map<String, Map<ConfigKey, Object>> config = new ConcurrentHashMap<>();
    private final List<Route> routes = new ArrayList<>();

    private final static String serviceRoute = "/soa/runtime/services" ;
    private final static String configRoute = "/soa/config/service" ;
    private final static String routesRoute = "/soa/config/route" ;

    private ZooKeeper zk;
    private String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;

    public ZookeeperWatcher(boolean isClient) {
        this.isClient = isClient;
    }

    public ZookeeperWatcher(boolean isClient, String zkHost) {
        this.isClient = isClient;
        this.zkHost = zkHost;
    }

    public void init() {
        connect();
        getRouteConfig(routesRoute);
        setConfigWatcher();
    }


    private void setConfigWatcher() {

        try {
            List<String> children = zk.getChildren(configRoute, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    setConfigWatcher();
                }
            });

            children.stream().filter(key -> config.containsKey(key)).forEach(this::getConfigData);
        } catch (KeeperException e) {
            LOGGER.error("get children of config root error");
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * 获取路由配置
     *
     * @param path
     */
    private void getRouteConfig(String path) {

        tryCreateNode(path);

        zk.getData(path, watchedEvent -> {

            if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                LOGGER.info(watchedEvent.getPath() + "'s data changed, reset route config in memory");
                getRouteConfig(watchedEvent.getPath());
            } else if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                LOGGER.info(watchedEvent.getPath() + " is deleted, reset route config in memory");
                routes.clear();
                getRouteConfig(watchedEvent.getPath());
            }
        }, (rc, path1, ctx, data, stat) -> {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    getRouteConfig(path1);
                    break;
                case OK:
                    processRouteDate(data);
                    break;
                default:
                    LOGGER.error("Error when trying to get data of {}.", path1);
            }
        }, path);
    }


    /**
     * 拿到路由配置信息，解析成Routes列表
     *
     * @param bytes
     */
    private void processRouteDate(byte[] bytes) {

        try {
            String data = new String(bytes, "utf-8");

            if (data.trim().equals("") || data.equals("/soa/config/route")) {
                routes.clear();
                return;
            }
            synchronized (routes) {
                routes.clear();
                new RouteParser().parseAll(routes, data);
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public List<Route> getRoutes() {
        return this.routes;
    }


    private void tryCreateNode(String path) {

        String[] paths = path.split("/");

        String createPath = "/" ;
        for (int i = 1; i < paths.length; i++) {
            createPath += paths[i];
            addPersistServerNode(createPath, path);
            createPath += "/" ;
        }
    }

    /**
     * 添加持久化的节点
     *
     * @param path
     * @param data
     */
    private void addPersistServerNode(String path, String data) {
        Stat stat = exists(path);

        if (stat == null)
            zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, nodeCreatedCallBack, data);
    }

    /**
     * 判断节点是否存在
     *
     * @param path
     * @return
     */
    private Stat exists(String path) {
        Stat stat = null;
        try {
            stat = zk.exists(path, false);
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }
        return stat;
    }

    /**
     * 异步添加serverName节点的回调处理
     */
    private AsyncCallback.StringCallback nodeCreatedCallBack = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("创建节点:{},连接断开，重新创建", path);
                tryCreateNode((String) ctx); //每次创建都会从根节点开始尝试创建，避免根节点未创建而造成创建失败
//                addPersistServerNode(path, (String) ctx);
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


    public void destroy() {
        if (zk != null) {
            try {
                LOGGER.info("{} 关闭到zookeeper的连接", isClient ? "Client's" : "Server's");
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public List<ServiceInfo> getServiceInfo(String serviceName, String versionName, boolean compatible) {

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
                usableList.addAll(serverList.stream().filter(server -> server.getVersionName().equals(versionName)).collect(Collectors.toList()));
            } else {
                usableList.addAll(serverList.stream().filter(server -> Version.toVersion(versionName).compatibleTo(Version.toVersion(server.getVersionName()))).collect(Collectors.toList()));
            }
        }
        return usableList;
    }

    public ServiceZKInfo getServiceZkInfo(String serviceName, Map<String, ServiceZKInfo> zkInfos) {
        String servicePath = serviceRoute + "/" + serviceName;
        try {
            if (zk == null)
                init();

            List<String> childrens = zk.getChildren(servicePath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    getServiceZkInfo(serviceName, zkInfos);
                }
            });

            List<RuntimeInstance> runtimeInstanceList = new ArrayList<>();
            LOGGER.info("获取{}的子节点成功", servicePath);
            //child = 10.168.13.96:9085:1.0.0
            for (String children : childrens) {
                String[] infos = children.split(":");
                RuntimeInstance instance = new RuntimeInstance(serviceName, infos[0], Integer.valueOf(infos[1]), infos[2]);
                runtimeInstanceList.add(instance);
            }
            ServiceZKInfo zkInfo = new ServiceZKInfo(serviceName, runtimeInstanceList);
            zkInfos.put(serviceName, zkInfo);
            return zkInfo;

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * 根据serviceName节点的路径，获取下面的子节点，并监听子节点变化
     *
     * @param serviceName
     */
    private void getServiceInfoByServiceName(String serviceName) {

        String servicePath = serviceRoute + "/" + serviceName;
        try {

            if (zk == null)
                init();

            List<String> children = zk.getChildren(servicePath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    getServiceInfoByServiceName(serviceName);
                }
            });

            LOGGER.info("获取{}的子节点成功", servicePath);
            WatcherUtils.resetServiceInfoByName(serviceName, servicePath, children, caches);

        } catch (KeeperException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 连接zookeeper
     */
    private void connect() {
        try {
            zk = new ZooKeeper(zkHost, 15000, e -> {
                if (e.getState() == Watcher.Event.KeeperState.Expired) {
                    LOGGER.info("{} 到zookeeper Server的session过期，重连", isClient ? "Client's" : "Server's");

                    destroy();
                    init();

                } else if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    LOGGER.info("{} Zookeeper Watcher 已连接 zookeeper Server", isClient ? "Client's" : "Server's");
                    tryCreateNode(serviceRoute);
                    tryCreateNode(configRoute);

                    caches.clear();
                    config.clear();

                } else if (e.getState() == Watcher.Event.KeeperState.Disconnected) {
                    LOGGER.error("{}到zookeeper的连接被断开", isClient ? "Client's" : "Server's");
                    destroy();
                    init();
                }
            });
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
    }

    public Map<ConfigKey, Object> getConfigWithKey(String serviceKey) {

        if (config.containsKey(serviceKey)) {
            return config.get(serviceKey);
        } else {
            getConfigData(serviceKey);
            return config.get(serviceKey);
        }
    }


    private void getConfigData(String configNodeName) {

        String configPath = configRoute + "/" + configNodeName;

        try {
            byte[] data = zk.getData(configPath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    LOGGER.info(watchedEvent.getPath() + "'s data changed, reset config in memory");
                    getConfigData(configNodeName);
                }
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                    LOGGER.info(watchedEvent.getPath() + " is deleted, remove config in memory");
                    config.remove(configNodeName);
                }
            }, null);

            WatcherUtils.processConfigData(configNodeName, data, config);

        } catch (KeeperException e) {
            LOGGER.error(e.getMessage());
            if (e instanceof KeeperException.NoNodeException) {
                config.put(configNodeName, new HashMap<>());
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    public static void main(String[] args) throws InterruptedException {
        ZookeeperWatcher zw = new ZookeeperWatcher(true);
        zw.init();
    }
}
