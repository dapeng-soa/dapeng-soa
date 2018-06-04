package com.github.dapeng.zookeeper.utils;

import com.github.dapeng.zookeeper.common.BaseZKClient;
import com.github.dapeng.zookeeper.utils.DataParseUtils.MonitorType;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 原生zk 监控
 *
 * @author huyj
 * @Created 2018/5/30 19:14
 */
public class NativeMonitorUtils {

    private static final Logger logger = LoggerFactory.getLogger(NativeMonitorUtils.class);


    /**
     * 添加 原生客户端 监听
     *
     * @param path
     * @param zooKeeper
     * @param zk_type
     * @param baseZKClient
     * @throws KeeperException
     * @throws InterruptedException
     */
    public static void MonitorNativeZkData(String path, ZooKeeper zooKeeper, BaseZKClient baseZKClient) throws KeeperException, InterruptedException {
        baseZKClient.lockZkDataContext();
        //如果没有要 创建 根节点
        if (Objects.isNull(zooKeeper.exists(path, false))) {
            zooKeeper.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        registerZkWatcher(path, zooKeeper, DataParseUtils.MonitorType.TYPE_ADDED, baseZKClient);
        baseZKClient.releaseZkDataContext();
    }

    /**
     * 注册 Watcher 事件
     *
     * @param path
     * @param zooKeeper
     * @param clientType
     * @param monitorType
     * @param baseZKClient
     */
    public static void registerZkWatcher(String path, ZooKeeper zooKeeper, MonitorType monitorType, BaseZKClient baseZKClient) {
        try {
            parseZkData(zooKeeper, path, baseZKClient, monitorType);
            if (Objects.nonNull(zooKeeper.exists(path, true))) {
                zooKeeper.getChildren(path, false).forEach(item -> {
                    String childPath = path + "/" + item;
                    registerZkWatcher(childPath, zooKeeper, monitorType, baseZKClient);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseZkData(ZooKeeper zooKeeper, String path, BaseZKClient baseZKClient, MonitorType monitorType) {
        if (monitorType != null) {
            String data = baseZKClient.getData(path);
            //同步ZK数据
            DataParseUtils.syncZkData(path, data, baseZKClient, monitorType, "Native");
        }

        /*一次性 监听事件  处理完后要再次注册*/
        try {
            zooKeeper.exists(path, watchedEvent -> {
                //logger.info(" 节点 [{}] 发生变化[{}]，正在同步信息....", watchedEvent.getPath(), watchedEvent.getType());
                MonitorType eventType = DataParseUtils.getChangeEventType(true, null, watchedEvent.getType());
                parseZkData(zooKeeper, path, baseZKClient, eventType);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
