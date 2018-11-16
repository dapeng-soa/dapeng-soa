package com.github.dapeng.registry.zookeeper2;

import com.github.dapeng.registry.zookeeper.CommonZk;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author ever maple
 */
public class ClientZkAgent extends CommonZk implements Watcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientZkAgent.class);

    private static final ClientZkAgent instance = new ClientZkAgent();

    private final Map<String, List<ZkServiceInfo>> serviceInfosByName = new ConcurrentHashMap<>(128);

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
        List serviceInfos = serviceInfosByName.get(serviceInfo.serviceName());

        if (serviceInfos == null) {
            serviceInfos = new LinkedList<>();
            serviceInfosByName.put(serviceInfo.serviceName(), serviceInfos);
        }

        serviceInfos.add(serviceInfo);


        startWatch(serviceInfo);
    }

    /**
     * 取消zk服务节点同步
     *
     * @param serviceInfo
     */
    public void cancel(ZkServiceInfo serviceInfo) {
        LOGGER.info("ClientZkAgent::cancel, serviceName:" + serviceInfo.serviceName());
        if (serviceInfosByName.containsKey(serviceInfo.serviceName())) {
            serviceInfosByName.get(serviceInfo.serviceName()).remove(serviceInfo);
        } else {
            LOGGER.warn("ClientZkAgent::cancel, no serviceInfo found for:" + serviceInfo.serviceName());
        }
    }

    /**
     * only handle NodeChildrenChanged for runtime nodes and NodeDataChanged for config nodes
     *
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        LOGGER.warn("ClientZkAgent::process, zkEvent: " + event);
        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
            String serviceName = event.getPath().substring(event.getPath().lastIndexOf("/") + 1);
            List<ZkServiceInfo> serviceInfos = serviceInfosByName.get(serviceName);
            if (serviceInfos != null && !serviceInfos.isEmpty()) {

            } else {
                LOGGER.warn("ClientZkAgent::process, no need to sync " + serviceName);
            }
//                if (zkServiceInfo.getStatus() != ZkServiceInfo.Status.OUT_OF_SYNC) {
//                    LOGGER.warn("{}::syncZkRuntimeInfo[{}]::子节点发生变化，重新获取信息,event:{}",
//                            getClass().getSimpleName(), zkServiceInfo.getService(), event);
//                    if (zkServiceInfo.getStatus() == ZkServiceInfo.Status.TRANSIENT) {
//                        zkServiceInfo.setStatus(ZkServiceInfo.Status.OUT_OF_SYNC);
//                    } else {
//                        ClientZk.getMasterInstance().syncZkRuntimeInfo(zkServiceInfo);
//                    }
//                }
        } else if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
//            if (zkServiceInfo.getStatus() != ZkServiceInfo.Status.OUT_OF_SYNC) {
//                LOGGER.warn("{}::syncZkConfigInfo[{}]::节点内容发生变化，重新获取配置信息,event:{}",
//                        getClass().getSimpleName(), zkServiceInfo.getService(), event);
//                if (zkServiceInfo.getStatus() == ZkServiceInfo.Status.TRANSIENT) {
//                    zkServiceInfo.setStatus(ZkServiceInfo.Status.OUT_OF_SYNC);
//                } else {
//                    ClientZk.getMasterInstance().syncZkConfigInfo(zkServiceInfo);
//                }
//            }
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
                        LOGGER.error("Client's host: {} 到zookeeper的连接被断开， do nothing.", zkHost);
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
                serviceInfosByName.clear();
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

    }
}
