package com.github.dapeng.registry.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author maple 2018.09.04 下午3:58
 */
public class ZkWatcher implements Watcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZkWatcher.class);
    private final ZkServiceInfo zkServiceInfo;


    public ZkWatcher(ZkServiceInfo zkServiceInfo) {
        this.zkServiceInfo = zkServiceInfo;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
            if (zkServiceInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                LOGGER.info("{}::syncZkRuntimeInfo[{}]:{}子节点发生变化，重新获取信息",
                        getClass().getSimpleName(), zkServiceInfo.service, event.getPath());

                ClientZk.getMasterInstance().syncZkRuntimeInfo(zkServiceInfo);
            }
        } else if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
            if (zkServiceInfo.getStatus() != ZkServiceInfo.Status.CANCELED) {
                LOGGER.info("{}::syncZkConfigInfo[{}]: {} 节点内容发生变化，重新获取配置信息",
                        getClass().getSimpleName(), zkServiceInfo.service, event.getPath());

                ClientZk.getMasterInstance().syncZkConfigInfo(zkServiceInfo);
            }
        }
    }
}
