package com.github.dapeng.impl.plugins.monitor;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.Application;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.RegistryAgent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author hui
 * @date 2018/11/29 0029 17:21
 */
public class CheckZkInfoScheduled {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckZkInfoScheduled.class);
    private final static CheckZkInfoScheduled instance = new CheckZkInfoScheduled();

    //周期 一天
    private final int PERIOD = 86400000;

    private final ScheduledExecutorService schedulerExecutorService = Executors.newScheduledThreadPool(1,
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("dapeng-" + getClass().getSimpleName() + "-scheduler-%d")
                    .build());

    private CheckZkInfoScheduled() {
        initThreads();
    }

    public static CheckZkInfoScheduled getInstance() {
        return instance;
    }


    private void initThreads() {
        schedulerExecutorService.scheduleWithFixedDelay(() -> {
            LOGGER.info("dapeng check zk node metadata started, interval:" + PERIOD + "ms");
            StringBuilder sb = new StringBuilder();
            Map<String, Stat> localZkNodeInfoMap = getZkLocalInfo();
            ZooKeeper zk = createZk();
            try {
                String result = checkZkNodeMetadata(zk, null, localZkNodeInfoMap);
                sb.append(result);
                LOGGER.info("the result of zkNodeInfo:" + sb.toString());
            } finally {
                if (zk != null) {
                    try {
                        zk.close();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }, 600000, PERIOD, TimeUnit.MILLISECONDS);
    }

    public Map<String, Stat> getZkLocalInfo() {
        LOGGER.info(getClass().getSimpleName() + " get zk localInfo...");
        Map<String, Stat> localZkNodeInfoMap = new ConcurrentHashMap<>(16);
        try {
            Map<String, Stat> localClientZkNodeInfo = new ConcurrentHashMap<>(16);
            Container container = ContainerFactory.getContainer();
            for (Application application : container.getApplications()) {
                Class<?> clientZkCl = application.getAppClasssLoader().loadClass("com.github.dapeng.registry.zookeeper.ClientZkAgent");
                Method getClientInstance = clientZkCl.getMethod("getInstance");
                Method getClientZkNodeInfo = clientZkCl.getMethod("getServiceZkNodeInfo");
                Map<String, Stat> appClientZkNodeInfo = (Map<String, Stat>) getClientZkNodeInfo.invoke(getClientInstance.invoke(clientZkCl));
                localClientZkNodeInfo.putAll(appClientZkNodeInfo);
            }

            Class<?> serverZk = container.getClass().getClassLoader().loadClass("com.github.dapeng.registry.zookeeper.ServerZkAgentImpl");
            Method getServerInstance = serverZk.getMethod("getInstance");
            RegistryAgent serverZkAgentImpl = (RegistryAgent) getServerInstance.invoke(serverZk);
            Method getServerZkNodeInfo = serverZk.getMethod("getServiceZkNodeInfo");
            Map<String, Stat> localServerZkNodeInfo = (Map<String, Stat>) getServerZkNodeInfo.invoke(serverZkAgentImpl);

            localZkNodeInfoMap.putAll(localServerZkNodeInfo);
            localZkNodeInfoMap.putAll(localClientZkNodeInfo);
            if (localZkNodeInfoMap.isEmpty()) {
                LOGGER.warn("本地zk元数据为空 \n");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return localZkNodeInfoMap;
    }

    public ZooKeeper createZk() {
        ZooKeeper zk = null;
        CountDownLatch connectedSignal = new CountDownLatch(1);
        try {
            zk = new ZooKeeper(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST, 30000, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            });
            connectedSignal.await(10000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return zk;
    }

    public String checkZkNodeMetadata(ZooKeeper zk, String path, Map<String, Stat> localZkNodeInfoMap) {
        StringBuilder sb = new StringBuilder();
        if (compareZkInfo(zk, path, localZkNodeInfoMap, sb)) {
            sb.append(" 本地元数据与服务端相同\n");
        } else {
            sb.append(" 本地元数据与服务端不同\n");
        }
        return sb.toString();
    }

    public boolean compareZkInfo(ZooKeeper zk, String path, Map<String, Stat> localZkNodeInfoMap, StringBuilder sb) {
        try {
            if (path == null) {
                for (String key : localZkNodeInfoMap.keySet()) {
                    StringBuilder localStr = new StringBuilder().append(localZkNodeInfoMap.get(key));
                    if(!compareStat(zk, key, localStr, sb)){
                        return false;
                    }
                }
            } else {
                StringBuilder localStr = new StringBuilder().append(localZkNodeInfoMap.get(path));
                //获取当前路径下服务端zk节点元数据
                Stat serverZkNodeInfo = zk.exists(path, false);
                if (serverZkNodeInfo == null) {
                    sb.append(path).append(" 该节点在服务端不存在\n");
                    return false;
                }
                if (localStr.toString().isEmpty()) {
                    sb.append(path).append(" 该节点不在本地缓存中\n");
                    return false;
                }

                if (!compareStat(zk, path, localStr, sb)) {
                    return false;
                }

                List<String> children = zk.getChildren(path, false, null);
                if (!children.isEmpty()) {
                    for (String child : children) {
                        String childPath = path + "/" + child;
                        if (!compareZkInfo(zk, childPath, localZkNodeInfoMap, sb)) {
                            return false;
                        }
                    }
                }
            }
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return true;
    }

    public boolean compareStat(ZooKeeper zk, String path, StringBuilder localStr, StringBuilder sb) {
        try {
            //获取当前路径下服务端zk节点元数据
            Stat serverNodeInfo = zk.exists(path, false);
            if (serverNodeInfo == null) {
                sb.append(path).append(" 该节点不存在\n");
                return false;
            }
            StringBuilder serviceStr = new StringBuilder().append(serverNodeInfo);
            //对比本地与服务端的节点元数据,如果相同则比较子节点的元数据
            if (!serviceStr.toString().equals(localStr.toString())) {
                sb.append("不相同的节点： ").append(path).append("\n");
                sb.append("本地缓存对应元数据： ").append(localStr).append("\n");
                sb.append("服务端对应元数据： ").append(serverNodeInfo).append("\n");
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return true;
    }
}
