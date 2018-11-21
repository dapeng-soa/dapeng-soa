package com.github.dapeng.registry.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZkUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZkUtils.class);

    /**
     * 递归节点创建
     */
    public static void create(String path, String data, RegisterContext context, boolean ephemeral, AsyncCallback.StringCallback callback, ZooKeeper zkClient) {

        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            //判断父节点是否存在...
            if (!exists(parentPath, zkClient)) {
                create(parentPath, "", null, false, null, zkClient);
            }
        }
        if (ephemeral) {
            createEphemeral(path + ":", data, context, callback, zkClient);
        } else {
            createPersistent(path, data, callback, zkClient);
        }
    }

    /**
     * 异步添加serverInfo,为临时有序节点，如果server挂了就木有了
     */
    public static void createEphemeral(String path, String data, RegisterContext context, AsyncCallback.StringCallback callback, ZooKeeper zkClient) {
        // 如果存在重复的临时节点， 删除之
        int i = path.lastIndexOf("/");
        if (i > 0) {
            String parentPath = path.substring(0, i);
            try {
                List<String> childNodes = zkClient.getChildren(parentPath, false);
                String _path = path.substring(i + 1);
                for (String nodeName : childNodes) {
                    if (nodeName.startsWith(_path)) {
                        zkClient.delete(parentPath + "/" + nodeName, -1);
                    }
                }
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error("ServerZk::createEphemeral delete exist nodes failed", e);
            }
        }
        //serverInfoCreateCallback
        zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, callback, context);
    }

    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    public static void createPersistent(String path, String data, AsyncCallback.StringCallback callback, ZooKeeper zkClient) {
        if (!exists(path, zkClient)) {
            zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, callback, data);
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
            LOGGER.error(ZkUtils.class + "::exists: " + t.getMessage(), t);
        }
        return false;
    }
}
