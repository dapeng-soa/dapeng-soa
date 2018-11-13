package com.github.dapeng.registry.zookeeper.watcher;

import com.github.dapeng.registry.zookeeper.ClientZk;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author maple 2018.09.04 下午3:58
 */
public class RoutesWatcher<T> implements Watcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesWatcher.class);

    private final String service;

    private final Map<String, List<T>> routesMap;

    private final RouteType type;

    public RoutesWatcher(String service, Map<String, List<T>> routesMap, RouteType type) {
        this.service = service;
        this.routesMap = routesMap;
        this.type = type;
    }

    public enum RouteType {
        SERVICE_ROUTE,
        COOKIE_RULE
    }

    @Override
    public void process(WatchedEvent event) {
        LOGGER.warn("RoutesWatcher::process zkEvent: " + event);
        if (event.getType() == Event.EventType.NodeDataChanged) {
            if (type == RouteType.SERVICE_ROUTE) {
                LOGGER.info("RoutesWatcher::watcher service  routes节点data发现变更,重新获取信息. event: {}", event);
                routesMap.remove(service);
                ClientZk.getMasterInstance().getRoutes(service);

            } else if (type == RouteType.COOKIE_RULE) {
                LOGGER.info("RoutesWatcher::watcher cookie rules 节点data发现变更,重新获取信息,event:{}", event);
                routesMap.remove(service);
                ClientZk.getMasterInstance().getCookieRules(service);
            }
        }

    }
}
