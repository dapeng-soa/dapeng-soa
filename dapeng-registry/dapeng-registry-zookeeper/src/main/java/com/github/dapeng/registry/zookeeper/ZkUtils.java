package com.github.dapeng.registry.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZkUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZkUtils.class);

    final static String RUNTIME_PATH = "/soa/runtime/services";
    final static String CONFIG_PATH = "/soa/config/services";
    final static String ROUTES_PATH = "/soa/config/routes";
    final static String FREQ_PATH = "/soa/config/freq";

    public static void syncZkConfigInfo(ZkServiceInfo zkInfo, ZooKeeper zk, Watcher watcher) {
        if (zk == null || !zk.getState().isConnected()) {
            LOGGER.warn(ZkUtils.class + "::syncZkConfigInfo zk is not ready, status:"
                    + (zk == null ? null : zk.getState()));
            return;
        }
        //1.获取 globalConfig  异步模式
        try {
            Stat stat = new Stat();
            byte[] data = zk.getData(CONFIG_PATH, watcher, stat);
            ServerZkAgentImpl.getInstance().getServiceZkNodeInfo().put(CONFIG_PATH, stat);
            ZkDataProcessor.processZkConfig(data, zkInfo, true);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("CommonZk::syncZkConfigInfo failed, zk status:" + zk.getState(), e);
        }

        // 2. 获取 service
        String configPath = CONFIG_PATH + "/" + zkInfo.serviceName();

        // zk config 有具体的service节点存在时，这一步在异步callback中进行判断
        try {
            Stat stat = new Stat();
            byte[] data = zk.getData(configPath, watcher, stat);
            ServerZkAgentImpl.getInstance().getServiceZkNodeInfo().put(configPath, stat);
            ZkDataProcessor.processZkConfig(data, zkInfo, false);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("CommonZk::syncZkConfigInfo failed, zk status:" + zk.getState(), e);
        }
    }
    /**
     * 异步添加serverInfo,为临时有序节点，如果server挂了就木有了
     */
    public static void createEphemeral(String path, String data, ZooKeeper zkClient) throws KeeperException, InterruptedException {
        // 如果存在重复的临时节点， 删除之。
        // 由于目前临时节点采用CreateMode.EPHEMERAL_SEQUENTIAL的方式， 会自动带有一个序号(ip:port:version:seq)，
        // 故判重方式需要比较(ip:port:version)即可
        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            createPersistNodeOnly(parentPath, zkClient);
            try {
                List<String> childNodes = zkClient.getChildren(parentPath, false);
                String _path = path.substring(i + 1);
                for (String nodeName : childNodes) {
                    if (nodeName.startsWith(_path)) {
                        zkClient.delete(parentPath + "/" + nodeName, -1);
                    }
                }
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("ServerZk::createEphemeral delete exist nodes failed, zk status:" + zkClient.getState(), e);
            }
        }
        //serverInfoCreateCallback
        zkClient.create(path + ":", data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    public static void createPersistent(String path, String data, ZooKeeper zkClient) throws KeeperException, InterruptedException {
        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            createPersistNodeOnly(parentPath, zkClient);
        }
        if (!exists(path, zkClient)) {
            zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    /**
     * 递归节点创建, 不监听
     */
    public static void createPersistNodeOnly(String path, ZooKeeper zkClient) {

        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            //判断父节点是否存在...
            if (!exists(parentPath, zkClient)) {
                createPersistNodeOnly(parentPath, zkClient);
            }
        }

        try {
            zkClient.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            if (e instanceof KeeperException.NodeExistsException) {
                LOGGER.info("ZkUtils::createPersistNodeOnly failed," + e.getMessage());
            } else {
                LOGGER.error("ZkUtils::createPersistNodeOnly failed, zk status:" + zkClient.getState(), e);
            }
        }
    }

    /**
     * 检查节点是否存在
     */
    public static boolean exists(String path, ZooKeeper zkClient) {
        try {
            Stat exists = zkClient.exists(path, false);
            return exists != null;
        } catch (Throwable t) {
            LOGGER.error(ZkUtils.class + "::exists check failed, zk status:" + zkClient.getState(), t);
        }
        return false;
    }
}
