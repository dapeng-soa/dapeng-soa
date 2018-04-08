package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    /**
     * zk 配置 缓存 ，根据 serivceName + versionName 作为 key
     */
    protected ConcurrentMap<String, ZkConfigInfo> zkConfigMap = new ConcurrentHashMap();

    /**
     * 获取zk 配置信息，封装到 ZkConfigInfo
     *
     * @param serviceName
     * @return
     */
    protected ZkConfigInfo getConfigData(String serviceName) {
        ZkConfigInfo info = zkConfigMap.get(serviceName);
        if (info != null) {
            return info;
        }

        ZkConfigInfo configInfo = new ZkConfigInfo();

        //1.获取 globalConfig
        try {
            byte[] globalData = zk.getData(CONFIG_PATH, watchedEvent -> {

                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    logger.info(watchedEvent.getPath() + "'s data changed, reset config in memory");
                    zkConfigMap.clear();
                    getConfigData(serviceName);
                }
            }, null);

            WatcherUtils.processZkConfig(globalData, configInfo, true);

        } catch (KeeperException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        // 2. 获取 service
        String configPath = CONFIG_PATH + "/" + serviceName;

        try {
            byte[] serviceData = zk.getData(configPath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    logger.info(watchedEvent.getPath() + "'s data changed, reset zkConfigMap in memory");
                    zkConfigMap.clear();

                    getConfigData(serviceName);
                }
            }, null);
            WatcherUtils.processZkConfig(serviceData, configInfo, false);

        } catch (KeeperException e) {
            logger.error(e.getMessage());
            if (e instanceof KeeperException.NoNodeException) {
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        zkConfigMap.put(serviceName, configInfo);

        return configInfo;
    }

}
