package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年03月22日 上午11:17
 */
public abstract class CommonZk {


    private static Logger LOGGER = LoggerFactory.getLogger(CommonZk.class);

    protected String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;


    protected final static String RUNTIME_PATH = "/soa/runtime/services";
    protected final static String CONFIG_PATH = "/soa/config/services";
    protected final static String ROUTES_PATH = "/soa/config/routes";
    protected final static String FREQ_PATH = "/soa/config/freq";


    protected ZooKeeper zk;

    public void init() {
        connect();
    }

    protected abstract void connect();
    /**
     * 保证zk watch机制，出现异常循环执行5次
     *
     * @param zkInfo
     */
    protected void syncZkRuntimeInfo(ZkServiceInfo zkInfo) {
        String servicePath = RUNTIME_PATH + "/" + zkInfo.service;
        int retry = 5;
        do {
            try {
                if (zk == null) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:zk is null, now init()");
                    init();
                }

                List<String> childrens;
                try {
                    childrens = zk.getChildren(servicePath, watchedEvent -> {
                        if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                            if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                                LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                                syncZkRuntimeInfo(zkInfo);
                            }
                        }
                    });
                } catch (KeeperException.NoNodeException e) {
                    LOGGER.error("sync service:  {} zk node is not exist,", zkInfo.service);
                    return;
                }

                if (childrens.size() == 0) {
                    zkInfo.setStatus(ZkServiceInfo.Status.CANCELED);
                    zkInfo.setRuntimeInstances(new ArrayList<>(8));
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:no service instances found");
                    return;
                }
                List<RuntimeInstance> runtimeInstanceList = new ArrayList<>(8);
                LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "], 获取{}的子节点成功", servicePath);
                //child = 10.168.13.96:9085:1:1.0.0
                for (String children : childrens) {
                    String[] infos = children.split(":");
                    RuntimeInstance instance = null;
                    //todo 兼容老版本 10.168.13.96:9085:1.0.0
                    if (infos[2].length() > 1) {
                        instance = new RuntimeInstance(zkInfo.service, infos[0], Integer.valueOf(infos[1]), infos[2]);
                    } else if (infos[2].equals("1")) {
                        instance = new RuntimeInstance(zkInfo.service, infos[0], Integer.valueOf(infos[1]), infos[3]);
                    } else {
                        LOGGER.info("Instance is not available, just skip:" + children);
                        continue;
                    }

                    runtimeInstanceList.add(instance);
                }

                zkInfo.setRuntimeInstances(runtimeInstanceList);
                StringBuilder logBuffer = new StringBuilder();
                zkInfo.getRuntimeInstances().forEach(info -> logBuffer.append(info.toString()));
                LOGGER.info("<-> syncZkRuntimeInfo 触发服务实例同步，目前服务实例列表:" + zkInfo.service + " -> " + logBuffer);
                zkInfo.setStatus(ZkServiceInfo.Status.ACTIVE);
                return;
            } catch (KeeperException | InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        } while (retry-- > 0);
    }


    private Watcher runtimeWatcher(ZkServiceInfo zkInfo) {
        return watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkRuntimeInfo[" + zkInfo.service + "]:{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    syncZkRuntimeInfo(zkInfo);
                }
            }
        };
    }

    protected void syncZkConfigInfo(ZkServiceInfo zkInfo) {
        //1.获取 globalConfig  异步模式
        zk.getData(CONFIG_PATH, watchedEvent -> {
            if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {

                if (zkInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                    LOGGER.info(getClass().getSimpleName() + "::syncZkConfigInfo[" + zkInfo.service + "]: {} 节点内容发生变化，重新获取配置信息", watchedEvent.getPath());
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
                LOGGER.info(watchedEvent.getPath() + "'s data changed, reset zkConfigMap in memory");
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
                LOGGER.info("{}子节点发生变化，重新获取子节点...", event.getPath());
            }
        }, nodeChildrenCb, null);

    }


    private AsyncCallback.StatCallback nodeChildrenCb = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("监听配置子节点时，session超时，重新监听", path);
                watchConfigServiceNodeChange();
                break;
            case OK:
                LOGGER.info("watch 监听配置子节点成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("watch监听配置子节点存在", path);
                break;
            default:
                LOGGER.info("创建节点:{},失败", path);
        }
    };

    /**
     * 全局配置异步getData
     */
    private AsyncCallback.DataCallback globalConfigDataCb = (rc, path, ctx, data, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.error("读取配置节点data时连接丢失，重新获取!");
                syncZkConfigInfo((ZkServiceInfo) ctx);
                break;
            case NONODE:
                LOGGER.error("全局配置节点不存在");
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
                LOGGER.error("服务 [{}] 的service配置节点不存在，无法获取service级配置信息 ", ((ZkServiceInfo) ctx).service);
                break;
            case OK:
                WatcherUtils.processZkConfig(data, (ZkServiceInfo) ctx, false);

                break;
            default:
                break;
        }
    };

}
