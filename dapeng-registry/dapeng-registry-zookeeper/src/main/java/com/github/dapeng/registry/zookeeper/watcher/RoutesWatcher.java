package com.github.dapeng.registry.zookeeper.watcher;

import com.github.dapeng.registry.zookeeper.ClientZk;
import com.github.dapeng.router.Route;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author maple 2018.09.04 下午3:58
 */
public class RoutesWatcher implements Watcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesWatcher.class);

    private final String service;

    private final Map<String, List<Route>> routesMap;

    public RoutesWatcher(String service, Map<String, List<Route>> routesMap) {
        this.service = service;
        this.routesMap = routesMap;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDataChanged) {
            LOGGER.info("RoutesWatcher::watcher routes节点data发现变更,重新获取信息. watcherEvent: {}", event);
            routesMap.remove(service);
            ClientZk.getMasterInstance().getRoutes(service);
        }
    }
}
