package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.version.Version;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.RuntimeInstance;
import com.github.dapeng.registry.ServiceInfo;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author tangliu
 * @date 2016/2/29
 */
public class ZookeeperWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperWatcher.class);

    private final boolean isClient;
    private final Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();
    /**
     * 其他配置信息
     */
    private final Map<String, Map<ConfigKey, Object>> config = new ConcurrentHashMap<>();
    /**
     * 路由配置信息
     */
    private final List<Route> routes = new ArrayList<>();

    private final static String SERVICE_PATH = "/soa/runtime/services";
    private final static String CONFIG_PATH = "/soa/config/service";
    private final static String ROUTES_PATH = "/soa/config/route";

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
        getRouteConfig(ROUTES_PATH);
        setConfigWatcher();
    }


    private void setConfigWatcher() {

        try {
            List<String> children = zk.getChildren(CONFIG_PATH, watchedEvent -> {
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

            if ("".equals(data.trim()) || ROUTES_PATH.equals(data)) {
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
                usableList.addAll(serverList.stream().filter(server -> server.versionName.equals(versionName)).collect(Collectors.toList()));
            } else {
                usableList.addAll(serverList.stream().filter(server -> Version.toVersion(versionName).compatibleTo(Version.toVersion(server.versionName))).collect(Collectors.toList()));
            }
        }
        return usableList;
    }

    public ServiceZKInfo getServiceZkInfo(String serviceName, Map<String, ServiceZKInfo> zkInfos) {
        String servicePath = SERVICE_PATH + "/" + serviceName;
        try {
            if (zk == null) {
                init();
            }

            List<String> childrens = zk.getChildren(servicePath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    if (zkInfos.containsKey(serviceName)) {
                        getServiceZkInfo(serviceName, zkInfos);
                    }
                }
            });

            if (childrens.size() == 0) {
                return null;
            }
            List<RuntimeInstance> runtimeInstanceList = new ArrayList<>();
            LOGGER.info("获取{}的子节点成功", servicePath);
            //child = 10.168.13.96:9085:1.0.0
            for (String children : childrens) {
                String[] infos = children.split(":");
                RuntimeInstance instance = new RuntimeInstance(serviceName, infos[0], Integer.valueOf(infos[1]), infos[2]);
                runtimeInstanceList.add(instance);
            }
            ServiceZKInfo zkInfo = new ServiceZKInfo(serviceName, runtimeInstanceList);
            // zkInfo config
            getConfigDataNew(serviceName, zkInfo);

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
            CountDownLatch semaphore = new CountDownLatch(1);

            // Fixme zk连接状态变化不应该引起本地runtime缓存的清除, 尤其是zk挂了之后, 不至于影响业务(本地缓存还存在于每个SoaConnectionPool中?)
            zk = new ZooKeeper(zkHost, 15000, e -> {
                switch (e.getState()) {
                    case Expired:
                        LOGGER.info("{} 到zookeeper Server的session过期，重连", isClient ? "Client's" : "Server's");

                        destroy();
                        init();
                        break;
                    case SyncConnected:
                        semaphore.countDown();
                        LOGGER.info("{} Zookeeper Watcher 已连接 zookeeper Server", isClient ? "Client's" : "Server's");
//                        tryCreateNode(SERVICE_PATH);
//                        tryCreateNode(CONFIG_PATH);

                        caches.clear();
                        config.clear();
                        break;
                    case Disconnected:
                        LOGGER.error("{}到zookeeper的连接被断开", isClient ? "Client's" : "Server's");
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

    // 要进行缓存
    public Map<ConfigKey, Object> getConfigWithKey(String serviceKey) {

        if (config.containsKey(serviceKey)) {
            return config.get(serviceKey);
        } else {
            getConfigData(serviceKey);
            return config.get(serviceKey);
        }
    }


    private void getConfigData(String configNodeName) {

        String configPath = CONFIG_PATH + "/" + configNodeName;

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

    /**
     * @param configNodeName
     */
    private void getConfigDataNew(String configNodeName, ServiceZKInfo zkInfo) {
        //1.获取 globalConfig
        try {
            byte[] globalData = zk.getData(CONFIG_PATH, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    LOGGER.info(watchedEvent.getPath() + "'s data changed, reset config in memory");
                    getConfigDataNew(configNodeName, zkInfo);
                }
            }, null);
            WatcherUtils.processConfigDataNew(globalData, zkInfo, true);

        } catch (KeeperException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }

        // 2. 获取 service
        String configPath = CONFIG_PATH + "/" + configNodeName;
        try {
            byte[] serviceData = zk.getData(configPath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    LOGGER.info(watchedEvent.getPath() + "'s data changed, reset config in memory");
                    getConfigDataNew(configNodeName, zkInfo);
                }
            }, null);
            WatcherUtils.processConfigDataNew(serviceData, zkInfo, false);

        } catch (KeeperException e) {
            LOGGER.error(e.getMessage());
            if (e instanceof KeeperException.NoNodeException) {
//                config.put(configNodeName, new HashMap<>());
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
