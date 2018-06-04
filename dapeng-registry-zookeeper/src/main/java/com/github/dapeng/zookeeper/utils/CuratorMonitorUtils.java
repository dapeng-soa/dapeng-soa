package com.github.dapeng.zookeeper.utils;

import com.github.dapeng.zookeeper.common.BaseZKClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Objects;


/**
 * zk 数据监听工具
 *
 * @author huyj
 * @Created 2018/5/25 9:48
 */
public class CuratorMonitorUtils {
    private static final Logger logger = LoggerFactory.getLogger(CuratorMonitorUtils.class);

    /*Curator client 监听*/
    public static void MonitorCuratorZkData(String path, CuratorFramework curator, BaseZKClient baseZKClient) throws Exception {
        TreeCache treeCache = new TreeCache(curator, path);
        treeCache.start();
        treeCache.getListenable().addListener((curatorFramework, treeCacheEvent) -> {
            if (Objects.isNull(treeCacheEvent.getData())) return;

            String changePath = treeCacheEvent.getData().getPath();
            String data = getNodeData(treeCacheEvent);
            DataParseUtils.MonitorType monitorType = DataParseUtils.getChangeEventType(false, treeCacheEvent, null);

            //同步ZK数据
            DataParseUtils.syncZkData(changePath, data, baseZKClient, monitorType, "Curator");
        });
    }

    private static String getNodeData(TreeCacheEvent treeCacheEvent) throws UnsupportedEncodingException {
        if (Objects.isNull(treeCacheEvent.getData().getData())) {
            return "";
        } else {
            return new String(treeCacheEvent.getData().getData(), "utf-8");
        }
    }
}
