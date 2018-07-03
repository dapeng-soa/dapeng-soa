package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年03月22日 上午11:17
 */
public class CommonZk {


    private static Logger logger = LoggerFactory.getLogger(CommonZk.class);

    protected String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;


    protected final static String RUNTIME_PATH = "/soa/runtime/services";
    protected final static String CONFIG_PATH = "/soa/config/services";
    protected final static String ROUTES_PATH = "/soa/config/routes";
    protected final static String FREQ_PATH = "/soa/config/freq";


    protected ZooKeeper zk;


    protected void syncZkConfigInfo(ZkServiceInfo zkInfo) {
        //1.获取 globalConfig  异步模式
        zk.getData(CONFIG_PATH, watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                    logger.info(getClass().getSimpleName() + "::syncZkConfigInfo[" + zkInfo.service + "]: {} 节点内容发生变化，重新获取配置信息", watchedEvent.getPath());
                    syncZkConfigInfo(zkInfo);
                }
            }
        }, globalConfigDataCb, zkInfo);

        //异步监听子节点变动
        watchConfigServiceNodeChange();

        // 2. 获取 service
        String configPath = CONFIG_PATH + "/" + zkInfo.service;

        // zk config 有具体的service节点存在时，这一步在异步callback中进行判断
        zk.getData(configPath, watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                logger.info(watchedEvent.getPath() + "'s data changed, reset zkConfigMap in memory");
                syncZkConfigInfo(zkInfo);
            }
        }, serviceConfigDataCb, zkInfo);
    }


    /**
     * 监听 "/soa/config/services" 下的子节点变动
     */
    private void watchConfigServiceNodeChange() {
        zk.exists(CONFIG_PATH, event -> {
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                logger.info("{}子节点发生变化，重新获取子节点...", event.getPath());
            }
        }, nodeChildrenCb, null);

    }


    private AsyncCallback.StatCallback nodeChildrenCb = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                logger.info("监听配置子节点时，session超时，重新监听", path);
                watchConfigServiceNodeChange();
                break;
            case OK:
                logger.info("watch 监听配置子节点成功", path);
                break;
            case NODEEXISTS:
                logger.info("watch监听配置子节点存在", path);
                break;
            default:
                logger.info("创建节点:{},失败", path);
        }
    };

    /**
     * 全局配置异步getData
     */
    private AsyncCallback.DataCallback globalConfigDataCb = (rc, path, ctx, data, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                logger.error("读取配置节点data时连接丢失，重新获取!");
                syncZkConfigInfo((ZkServiceInfo) ctx);
                break;
            case NONODE:
                logger.error("全局配置节点不存在");
                break;
            case OK:
                WatcherUtils.processZkConfig(data, (ZkServiceInfo) ctx, true);
                break;
            default:
                break;
        }
    };

    /**
     * service级别异步 getData
     */
    private AsyncCallback.DataCallback serviceConfigDataCb = (rc, path, ctx, data, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                syncZkConfigInfo((ZkServiceInfo) ctx);
                break;
            case NONODE:
                logger.error("服务 [{}] 的service配置节点不存在，无法获取service级配置信息 ", ((ZkServiceInfo) ctx).service);
                break;
            case OK:
                WatcherUtils.processZkConfig(data, (ZkServiceInfo) ctx, false);

                break;
            default:
                break;
        }
    };

}
