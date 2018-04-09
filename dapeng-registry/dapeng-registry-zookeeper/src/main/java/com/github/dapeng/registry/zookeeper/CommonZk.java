package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年03月22日 上午11:17
 */
public class CommonZk {


    private static Logger logger = LoggerFactory.getLogger(CommonZk.class);

    protected String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;


    protected final static String SERVICE_PATH = "/soa/runtime/services";
    protected final static String CONFIG_PATH = "/soa/config/services";
    protected final static String ROUTES_PATH = "/soa/config/routes";


    protected ZooKeeper zk;


    protected void syncZkConfigInfo(ZkServiceInfo zkInfo) {
        //1.获取 globalConfig
        try {
            byte[] globalData = zk.getData(CONFIG_PATH, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {

                    if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                        logger.info(getClass().getSimpleName() + "::syncZkConfigInfo[" + zkInfo.service + "]: {} 节点内容发生变化，重新获取配置信息", watchedEvent.getPath());
                        syncZkConfigInfo(zkInfo);
                    }
                }
            }, null);
            WatcherUtils.processZkConfig(globalData, zkInfo, true);
        } catch (KeeperException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        // 2. 获取 service
        String configPath = CONFIG_PATH + "/" + zkInfo.service;

        try {
            // zk config 有具体的service节点存在时，才会进行下一步
            if (zk.exists(configPath, false) != null) {
                byte[] serviceData = zk.getData(configPath, watchedEvent -> {
                    if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                        logger.info(watchedEvent.getPath() + "'s data changed, reset zkConfigMap in memory");
                        syncZkConfigInfo(zkInfo);
                    }
                }, null);
                WatcherUtils.processZkConfig(serviceData, zkInfo, false);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error(e.getMessage());
        }
    }


}
