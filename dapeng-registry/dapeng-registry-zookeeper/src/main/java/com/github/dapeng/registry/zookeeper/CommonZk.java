package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 描述: common zookeeper client
 *
 * @author hz.lei
 * @date 2018年03月22日 上午11:17
 */
public class CommonZk {
    private static Logger logger = LoggerFactory.getLogger(CommonZk.class);

    String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;

    final static String RUNTIME_PATH = "/soa/runtime/services";
    final static String CONFIG_PATH = "/soa/config/services";
    final static String ROUTES_PATH = "/soa/config/routes";
    final static String FREQ_PATH = "/soa/config/freq";

    protected ZooKeeper zk;

    public void syncZkConfigInfo(ZkServiceInfo zkInfo) {
        //1.获取 globalConfig  异步模式
        zk.getData(CONFIG_PATH, zkInfo.getWatcher(), globalConfigDataCb, zkInfo);

        //异步监听子节点变动
//        watchConfigServiceNodeChange();

        // 2. 获取 service
        String configPath = CONFIG_PATH + "/" + zkInfo.getService();

        // zk config 有具体的service节点存在时，这一步在异步callback中进行判断
        zk.getData(configPath, zkInfo.getWatcher(), serviceConfigDataCb, zkInfo);
    }


    /**
     * 监听 "/soa/config/services" 下的子节点变动
     */
    /*private void watchConfigServiceNodeChange() {
        zk.exists(CONFIG_PATH, configServiceNodeChangeWatcher, nodeChildrenCb, null);

    }*/

    /*private Watcher configServiceNodeChangeWatcher = event -> {
        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
            logger.info("{}子节点发生变化，重新获取子节点...", event.getPath());
        }
    };*/

    /*private AsyncCallback.StatCallback nodeChildrenCb = (rc, path, ctx, name) -> {
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
    };*/

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
                logger.error("服务 [{}] 的service配置节点不存在，无法获取service级配置信息 ", ((ZkServiceInfo) ctx).getService());
                break;
            case OK:
                WatcherUtils.processZkConfig(data, (ZkServiceInfo) ctx, false);
                break;
            default:
                break;
        }
    };

}
