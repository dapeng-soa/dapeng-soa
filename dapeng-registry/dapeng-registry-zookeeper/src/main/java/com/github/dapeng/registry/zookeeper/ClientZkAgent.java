package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author ever maple
 */
public class ClientZkAgent extends CommonZk {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientZkAgent.class);

    private static final ClientZkAgent instance = new ClientZkAgent();

    private final Map<String, ZkServiceInfo> serviceInfoByName = new ConcurrentHashMap<>(128);

    private ClientZkAgent() {
        init();
    }

    public static ClientZkAgent getInstance() {
        return instance;
    }

    /**
     * 同步zk服务节点信息
     *
     * @param serviceInfo
     */
    public void sync(ZkServiceInfo serviceInfo) {
        synchronized (serviceInfoByName) {
            serviceInfoByName.put(serviceInfo.serviceName(), serviceInfo);
        }
        startWatch(serviceInfo);
    }

    /**
     * 取消zk服务节点同步
     *
     * @param serviceInfo
     */
    public void cancel(ZkServiceInfo serviceInfo) {
        LOGGER.info("ClientZkAgent::cancel, serviceName:" + serviceInfo.serviceName());
        synchronized (serviceInfoByName) {
            // 1
            ZkServiceInfo oldServiceInfo = serviceInfoByName.get(serviceInfo.serviceName());

            if (oldServiceInfo != null && serviceInfo == oldServiceInfo) {
                // 2, 步骤1跟2之间， serviceInfosByName可能会发生变化， 所以需要做同步
                serviceInfoByName.remove(serviceInfo.serviceName());
                LOGGER.info("ClientZkAgent::cancel succeed, serviceName:" + serviceInfo.serviceName());
            } else {
                LOGGER.warn("ClientZkAgent::cancel, no serviceInfo found for:" + serviceInfo.serviceName());
            }
        }
    }

    public ZkServiceInfo serviceInfo(String serviceName) {
        return serviceInfoByName.get(serviceName);
    }

    /**
     * only handle NodeChildrenChanged for runtime nodes and NodeDataChanged for config nodes
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        LOGGER.warn("ClientZkAgent::process, zkEvent: " + event);
        String serviceName = event.getPath().substring(event.getPath().lastIndexOf("/") + 1);
        ZkServiceInfo serviceInfo = serviceInfoByName.get(serviceName);
        if (serviceInfo == null) {
            LOGGER.warn("ClientZkAgent::process, no need to sync any more: " + serviceName);
            return;
        }
        switch (event.getType()) {
            case NodeChildrenChanged:
                syncZkRuntimeInfo(serviceInfo);
                break;
            case NodeDataChanged:
                if (event.getPath().startsWith(CONFIG_PATH)) {
                    syncZkConfigInfo(serviceInfo);
                } else if (event.getPath().startsWith(ROUTES_PATH)) {
                    syncZkRouteInfo(serviceInfo);
                }
                break;
            default:
                LOGGER.warn("ClientZkAgent::process Just ignore this event.");
                break;
        }
    }

    private void init() {
        connect();
    }

    /**
     * 连接zookeeper
     */
    private void connect() {
        try {
            CountDownLatch semaphore = new CountDownLatch(1);

            // default watch
            zk = new ZooKeeper(zkHost, 30000, e -> {
                LOGGER.info("ClientZk::connect zkEvent:" + e);
                switch (e.getState()) {
                    case Expired:
                        LOGGER.info("Client's host: {} 到zookeeper Server的session过期，重连", zkHost);
                        destroy();
                        init();
                        break;
                    case SyncConnected:
                        semaphore.countDown();
                        LOGGER.info("Client's host: {}  已连接 zookeeper Server", zkHost);
                        break;
                    case Disconnected:
                        LOGGER.error("Client's host: {} 到zookeeper的连接被断开， just ignore.", zkHost);
                        break;
                    case AuthFailed:
                        LOGGER.error("Zookeeper connection auth failed ...");
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
                serviceInfoByName.clear();
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 同步zk信息
     *
     * @param serviceInfo
     */
    private void startWatch(ZkServiceInfo serviceInfo) {
        LOGGER.info(getClass().getSimpleName() + "::syncServiceZkInfo[serviceName:" + serviceInfo.serviceName() + "], runtimeInstants:" + serviceInfo.runtimeInstances().size());
        try {
            // sync runtimeList
            syncZkRuntimeInfo(serviceInfo);
            // sync router config
            syncZkRouteInfo(serviceInfo);
            // sync service config
            syncZkConfigInfo(serviceInfo);

            LOGGER.info(getClass().getSimpleName() + "::syncServiceZkInfo[serviceName:" + serviceInfo.serviceName() + "]:zkInfo succeed, runtimeInstants:" + serviceInfo.runtimeInstances().size());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.error(getClass().getSimpleName() + "::syncServiceZkInfo[serviceName:" + serviceInfo.serviceName() + "]:zkInfo failed, runtimeInstants:" + serviceInfo.runtimeInstances().size());
        }
    }

    /**
     * 保证zk watch机制，出现异常循环执行5次
     */
    private void syncZkRuntimeInfo(ZkServiceInfo serviceInfo) {
        String servicePath = RUNTIME_PATH + "/" + serviceInfo.serviceName();
        int retry = 5;
        do {
            try {
                if (zk == null) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + serviceInfo.serviceName() + "]:zk is null, now init()");
                    init();
                }

                List<String> childrens;
                try {
                    childrens = zk.getChildren(servicePath, this);
                } catch (KeeperException.NoNodeException e) {
                    LOGGER.error("sync service:  " + serviceInfo.serviceName() + " zk node is not exist");
                    return;
                }

                if (childrens.size() == 0) {
                    serviceInfo.runtimeInstances().clear();
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo["
                            + serviceInfo.serviceName() + "]:no service instances found");
                    return;
                }

                LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo["
                        + serviceInfo.serviceName() + "], 获取" + servicePath + "的子节点成功");
                List<RuntimeInstance> runtimeInstances = new ArrayList<>(8);
                //child = 10.168.13.96:9085:1.0.0:0000000300
                for (String children : childrens) {
                    String[] infos = children.split(":");
                    RuntimeInstance instance = new RuntimeInstance(serviceInfo.serviceName(),
                            infos[0], Integer.valueOf(infos[1]), infos[2]);
                    runtimeInstances.add(instance);
                }

                // copyOnWriteArrayList
                List<RuntimeInstance> runtimeInstanceList = serviceInfo.runtimeInstances();
                //这里要clear掉，因为接下来会重新将实例信息放入list中，不清理会导致重复...
                runtimeInstanceList.clear();
                runtimeInstanceList.addAll(runtimeInstances);

                LOGGER.info("ClientZk::syncZkRuntimeInfo 触发服务实例同步，目前服务实例列表: "
                        + serviceInfo.serviceName() + " -> " + serviceInfo.runtimeInstances());
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

    /**
     * route 根据给定路由规则对可运行实例进行过滤
     */
    private void syncZkRouteInfo(ZkServiceInfo serviceInfo) {
        LOGGER.warn("ClientZKAgent::syncZkRouteInfo routesMap service:" + serviceInfo.serviceName());
        String servicePath = ROUTES_PATH + "/" + serviceInfo.serviceName();
        try {
            byte[] data = zk.getData(servicePath, this, null);
            processRouteData(serviceInfo, data);
            LOGGER.warn("ClientZk::getRoutes routes changes:" + serviceInfo.routes());
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("获取route service 节点: " + serviceInfo.serviceName() + " 出现异常");
        }
    }

    /**
     * process zk data 解析route 信息
     */
    private void processRouteData(ZkServiceInfo serviceInfo, byte[] data) {
        try {
            String routeData = new String(data, StandardCharsets.UTF_8);
            List<Route> zkRoutes = RoutesExecutor.parseAll(routeData);
            serviceInfo.routes(zkRoutes);
        } catch (Exception e) {
            LOGGER.error("parser routes 信息 失败，请检查路由规则写法是否正确:" + e.getMessage());
        }
    }
}
