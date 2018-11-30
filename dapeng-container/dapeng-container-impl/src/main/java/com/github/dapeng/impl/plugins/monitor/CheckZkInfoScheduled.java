package com.github.dapeng.impl.plugins.monitor;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.registry.zookeeper.ClientZkAgent;
import com.github.dapeng.registry.zookeeper.ServerZkAgentImpl;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author hui
 * @date 2018/11/29 0029 17:21
 */
public class CheckZkInfoScheduled {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckZkInfoScheduled.class);
    private final static String RUNTIME_PATH = "/soa/runtime/services";
    private final static String CONFIG_PATH = "/soa/config/services";
    private final static String ROUTES_PATH = "/soa/config/routes";
    private final static String FREQ_PATH = "/soa/config/freq";
    private final static CheckZkInfoScheduled instance = new CheckZkInfoScheduled();
    private final static List<String> checkList = new ArrayList<String>() {{
        add(RUNTIME_PATH);
        add(CONFIG_PATH);
        add(ROUTES_PATH);
        add(FREQ_PATH);
    }};

    //周期 一天
    private final int PERIOD = 60000;

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
        LOGGER.info("dapeng check zk node metadata started, interval:" + PERIOD + "ms");
        schedulerExecutorService.scheduleWithFixedDelay(() -> {
            LOGGER.info("::schedulerExecutorService exec............");
            StringBuilder sb = new StringBuilder();
            Map<String, Stat> localZkNodeInfoMap = getZkLocalInfo(sb);
            ZooKeeper zk = createZk();
            try {
                for (String path : checkList) {
                    checkZkNodeMetadata(zk, path, localZkNodeInfoMap, sb);
                    LOGGER.info("the result of zkNodeInfo:" + sb);
                }
            } finally {
                if (zk != null) {
                    try {
                        zk.close();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }, 20000, PERIOD, TimeUnit.MILLISECONDS);
    }

    public Map<String, Stat> getZkLocalInfo(StringBuilder sb) {
        Map<String, Stat> localZkNodeInfoMap = new ConcurrentHashMap<>(16);
        Map<String, Stat> localServerZkNodeInfo = ServerZkAgentImpl.getInstance().getServiceZkNodeInfo();
        Map<String, Stat> localClientZkNodeInfo = ClientZkAgent.getInstance().getServiceZkNodeInfo();
        localZkNodeInfoMap.putAll(localServerZkNodeInfo);
        localZkNodeInfoMap.putAll(localClientZkNodeInfo);
        sb.append("本地zk元数据：\n");
        if (!localZkNodeInfoMap.isEmpty()) {
            for (String key : localZkNodeInfoMap.keySet()) {
                Stat info = localZkNodeInfoMap.get(key);
                sb.append(key).append("==>").append(info).append("\n");
            }
        } else {
            sb.append("本地zk元数据为空 \n");
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

    public void checkZkNodeMetadata(ZooKeeper zk, String path, Map<String, Stat> localZkNodeInfoMap, StringBuilder sb) {
       // StringBuilder sb = new StringBuilder();
        if (compareZkInfo(zk, path, localZkNodeInfoMap, sb)) {
            sb.append("本地zk节点元数据与服务端相同\n");
        } else {
            sb.append("本地zk节点元数据与服务端不同\n");
        }
       // return sb.toString();
    }

    public boolean compareZkInfo(ZooKeeper zk, String path, Map<String, Stat> localZkNodeInfoMap, StringBuilder sb) {
        try {
            //获取当前路径下服务端zk节点元数据
            Stat serverZkNodeInfo = zk.exists(path, false);
            if (serverZkNodeInfo == null) {
                sb.append(path).append(" 该节点不存在\n");
                return false;
            }

            sb.append("检验的节点和对应元数据:\n");
            sb.append(path).append("==>").append(serverZkNodeInfo).append("\n");

            //对比本地与服务端的节点元数据,如果相同则比较子节点的元数据
            if (!serverZkNodeInfo.equals(localZkNodeInfoMap.get(path))) {
                sb.append("不相同的节点和对应元数据： ").append(path).append("==>").append(serverZkNodeInfo).append("\n");
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
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return true;
    }
}
