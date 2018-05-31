package com.github.dapeng.zookeeper.utils;

import com.github.dapeng.zookeeper.common.BaseZKClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import static com.github.dapeng.zookeeper.common.BaseConfig.*;


/**
 * zk 数据监听工具
 *
 * @author huyj
 * @Created 2018/5/25 9:48
 */
public class CuratorMonitorUtils {
    private static final Logger logger = LoggerFactory.getLogger(CuratorMonitorUtils.class);

    /*Curator client 监听*/
    public static void MonitorCuratorZkData(String path, CuratorFramework curator, BaseZKClient.ZK_TYPE zk_type, BaseZKClient baseZKClient) throws Exception {
        TreeCache treeCache = new TreeCache(curator, path);
        treeCache.start();
        treeCache.getListenable().addListener((curatorFramework, treeCacheEvent) -> {
            if (Objects.isNull(treeCacheEvent.getData())) return;

            String changePath = treeCacheEvent.getData().getPath();
            DataParseUtils.MonitorType monitorType = DataParseUtils.getChangeEventType(false, treeCacheEvent, null);
            switch (DataParseUtils.getChangePath(changePath)) {
                //运行实例
                case MONITOR_RUNTIME_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    DataParseUtils.runtimeInstanceChanged(monitorType, changePath, baseZKClient.zkDataContext(), zk_type, baseZKClient);
                    System.out.println("---- Curator " + zk_type + " ------ ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                //服务配置
                case CONFIG_PATH:
                    //  baseZKClient.lockZkDataContext();
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    String configData = getNodeData(treeCacheEvent);
                    DataParseUtils.configsDataChanged(changePath, configData, baseZKClient.zkDataContext());
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    // baseZKClient.releaseZkDataContext();
                    break;

                //路由配置
                case MONITOR_ROUTES_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    String routeData = getNodeData(treeCacheEvent);
                    DataParseUtils.routesDataChanged(changePath, routeData, baseZKClient.zkDataContext());
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    // baseZKClient.releaseZkDataContext();
                    break;

                //限流规则
                case MONITOR_FREQ_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    String freqData = getNodeData(treeCacheEvent);
                    DataParseUtils.freqsDataChanged(changePath, freqData, baseZKClient.zkDataContext());
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                //白名单
                case MONITOR_WHITELIST_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    DataParseUtils.whiteListChanged(monitorType, changePath, baseZKClient.zkDataContext());
                    System.out.println("---- Curator " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                default:
                    logger.info("The current path[{}] is not monitored....", DataParseUtils.getChangePath(changePath));
                    break;
            }
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
