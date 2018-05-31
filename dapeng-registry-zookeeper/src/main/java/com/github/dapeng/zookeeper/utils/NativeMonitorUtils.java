package com.github.dapeng.zookeeper.utils;

import com.github.dapeng.zookeeper.common.BaseZKClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * 原生zk 监控
 *
 * @author huyj
 * @Created 2018/5/30 19:14
 */
public class NativeMonitorUtils {

    private static final Logger logger = LoggerFactory.getLogger(NativeMonitorUtils.class);


    private static void getMonitorNativeZkData(String path, ZooKeeper zooKeeper, BaseZKClient.ZK_TYPE zk_type, BaseZKClient baseZKClient) {
        try {
            List<String> data = zooKeeper.getChildren(path, watchedEvent -> {
                System.out.println("回调getMonitorNativeZkData实例： 路径" + watchedEvent.getPath() + " 类型：" + watchedEvent.getType());
                getMonitorNativeZkData(path, zooKeeper, zk_type, baseZKClient);
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    getMonitorNativeZkData(path, zooKeeper, zk_type, baseZKClient);
                }
            }, null);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            logger.error("监听zk 数据异常节点出现异常[{}],Cause [{}]", e.getMessage(), e.getCause());
        }
    }

    /*Curator client 监听*/
    public static void MonitorNativeZkData(String path, ZooKeeper zooKeeper, BaseZKClient.ZK_TYPE zk_type, BaseZKClient baseZKClient) {

        getMonitorNativeZkData(path, zooKeeper, zk_type, baseZKClient);

        /*zooKeeper.register((WatchedEvent watchedEvent) -> {

            System.out.println("-------------------------------位置：NativeMonitorUtils.MonitorNativeZkData ==> " + "[watchedEvent = " + watchedEvent + "]");
            if (Objects.isNull(watchedEvent.getPath())) return;
            String changePath = watchedEvent.getPath();
            DataParseUtils.MonitorType monitorType = DataParseUtils.getChangeEventType(true, null, watchedEvent.getType());
            switch (DataParseUtils.getChangePath(changePath)) {
                //运行实例
                case MONITOR_RUNTIME_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    DataParseUtils.runtimeInstanceChanged(monitorType, changePath, baseZKClient.zkDataContext(), zk_type, baseZKClient);
                    System.out.println("---- Native " + zk_type + " ------ ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                //服务配置
                case CONFIG_PATH:
                    //  baseZKClient.lockZkDataContext();
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    String configData = getNodeData(baseZKClient, changePath);
                    DataParseUtils.configsDataChanged(changePath, configData, baseZKClient.zkDataContext());
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    // baseZKClient.releaseZkDataContext();
                    break;

                //路由配置
                case MONITOR_ROUTES_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    String routeData = getNodeData(baseZKClient, changePath);
                    DataParseUtils.routesDataChanged(changePath, routeData, baseZKClient.zkDataContext());
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    // baseZKClient.releaseZkDataContext();
                    break;

                //限流规则
                case MONITOR_FREQ_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    String freqData = getNodeData(baseZKClient, changePath);
                    DataParseUtils.freqsDataChanged(changePath, freqData, baseZKClient.zkDataContext());
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                //白名单
                case MONITOR_WHITELIST_PATH:
                    //baseZKClient.lockZkDataContext();
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 开始同步 -------");
                    DataParseUtils.whiteListChanged(monitorType, changePath, baseZKClient.zkDataContext());
                    System.out.println("---- Native " + zk_type + " ---- ZK [" + changePath + "] 同步结束 ------- ");
                    //baseZKClient.releaseZkDataContext();
                    break;

                default:
                    logger.info("The current path[{}] is not monitored....", DataParseUtils.getChangePath(changePath));
                    break;
            }
        });*/
    }

    private static String getNodeData(BaseZKClient baseZKClient, String path) {
        try {
            byte[] data = baseZKClient.getData(path);
            if (Objects.isNull(data)) {
                return "";
            } else {
                return new String(data, "utf-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
