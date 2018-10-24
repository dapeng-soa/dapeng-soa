package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.cookie.CookieExecutor;
import com.github.dapeng.cookie.CookieRoute;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.version.Version;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
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

    private final Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>(128);
    /**
     * 其他配置信息
     */
    private final Map<String, Map<ConfigKey, Object>> config = new ConcurrentHashMap<>(128);
    /**
     * 路由配置信息
     */
    private final Map<String, List<Route>> routesMap = new ConcurrentHashMap<>(128);

    /**
     * cookie 路由配置信息
     */
    private final Map<String, List<CookieRoute>> cookieRoutesMap = new ConcurrentHashMap<>(32);

    /**
     * master zkHost
     */
    private static final String MASTER_HOST = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;
    /**
     * fallback zkHost
     */
    private static final String FALLBACK_HOST = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_HOST;


    private static ClientZk clientZk;
    private static ClientZk fallbackZk;

    private ClientZk(String zkHost) {
        this.zkHost = zkHost;
        init();
    }

    private void init() {
        connect();
    }

    /**
     * 工厂方法获取zk实例
     */
    public static ClientZk getMasterInstance() {
        if (clientZk == null) {
            synchronized (ClientZk.class) {
                if (clientZk == null) {
                    clientZk = new ClientZk(MASTER_HOST);
                }
            }
        }
        return clientZk;
    }

    /**
     * 工厂方法，获取 fallback client zk 实例
     */
    public static ClientZk getFallbackInstance() {
        if (fallbackZk == null) {
            synchronized (ClientZk.class) {
                if (fallbackZk == null) {
                    fallbackZk = new ClientZk(FALLBACK_HOST);
                }
            }
        }
        return clientZk;
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
    public List<Route> getRoutes(String service) {
        if (routesMap.get(service) == null) {
            try {
                byte[] data = zk.getData(ROUTES_PATH + "/" + service, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                        LOGGER.info("routes 节点 data 发现变更，重新获取信息");
                        routesMap.remove(service);
                        getRoutes(service);
                    }
                }, null);
                List<Route> routes = processRouteData(service, data);
                LOGGER.warn("ClientZk::getRoutes routes changes:" + routes);
                return routes;
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("获取route service 节点: {} 出现异常", service);
            }
        } else {
            LOGGER.debug("获取route信息, service: {} , route size {}", service, routesMap.get(service).size());
            return this.routesMap.get(service);
        }
        return null;
    }

    public List<CookieRoute> getCookieRoutes(String service) {
        if (cookieRoutesMap.get(service) == null) {
            try {
                byte[] data = zk.getData(COOKIE_ROUTES_PATH + "/" + service, event -> {
                    if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
                        LOGGER.info("cookie routes 节点 data 发现变更，重新获取信息");
                        cookieRoutesMap.remove(service);
                        getCookieRoutes(service);
                    }
                }, null);
                List<CookieRoute> routes = processCookieRouteData(service, data);
                LOGGER.warn("ClientZk::getCookieRoutes routes changes:" + routes);
                return routes;
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("获取route service 节点: {} 出现异常", service);
            }
        } else {
            LOGGER.debug("获取 cookie route信息, service: {} , route size {}", service, cookieRoutesMap.get(service).size());
            return this.cookieRoutesMap.get(service);
        }
        return null;
    }


    /**
     * process zk data 解析route 信息
     */
    public List<Route> processRouteData(String service, byte[] data) {
        List<Route> zkRoutes;
        try {
            String routeData = new String(data, StandardCharsets.UTF_8);
            zkRoutes = RoutesExecutor.parseAll(routeData);
            routesMap.put(service, zkRoutes);
        } catch (Exception e) {
            zkRoutes = new ArrayList<>(16);
            LOGGER.error("parser routes 信息 失败，请检查路由规则写法是否正确!");
        }
        return zkRoutes;
    }

    /**
     * process zk data 解析route 信息
     */
    public List<CookieRoute> processCookieRouteData(String service, byte[] data) {
        List<CookieRoute> zkRoutes;
        try {
            String routeData = new String(data, StandardCharsets.UTF_8);
            zkRoutes = CookieExecutor.parseCookieRoutes(routeData);
            cookieRoutesMap.put(service, zkRoutes);
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
    public void syncZkRuntimeInfo(ZkServiceInfo zkInfo) {
        String servicePath = RUNTIME_PATH + "/" + zkInfo.service;
        int retry = 5;
        do {
            try {
                if (zk == null) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:zk is null, now init()");
                    init();
                }

                List<String> childrens;
                try {
                    childrens = zk.getChildren(servicePath, zkInfo.getWatcher());
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

                LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "], 获取{}的子节点成功", servicePath);
                List<RuntimeInstance> runtimeInstances = new ArrayList<>(8);
                //child = 10.168.13.96:9085:1.0.0
                for (String children : childrens) {
                    String[] infos = children.split(":");
                    RuntimeInstance instance = new RuntimeInstance(zkInfo.service, infos[0], Integer.valueOf(infos[1]), infos[2]);
                    runtimeInstances.add(instance);
                }

                // copyOnWriteArrayList
                List<RuntimeInstance> runtimeInstanceList = zkInfo.getRuntimeInstances();
                //这里要clear掉，因为接下来会重新将实例信息放入list中，不清理会导致重复...
                runtimeInstanceList.clear();
                runtimeInstanceList.addAll(runtimeInstances);

                LOGGER.info("<-> syncZkRuntimeInfo 触发服务实例同步，目前服务实例列表: {} -> {}",
                        zkInfo.service, zkInfo.getRuntimeInstances().toString());
                zkInfo.setStatus(ZkServiceInfo.Status.ACTIVE);
                return;
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        } while (retry-- > 0);
    }
}
