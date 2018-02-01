package com.github.dapeng.route;


import com.github.dapeng.core.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;


public final class RouteEngineFactory {

    private final static Logger LOG = LoggerFactory.getLogger(RouteEngineFactory.class);

    private static RouteEngine engine;

    public static synchronized RouteEngine getRouteEngine() {
        if (engine != null)
            return engine;

        try {
            Class<?> engineImpl = Class.forName("com.github.dapeng.soa.route.RouteEngineImpl");
            engine = (RouteEngine) engineImpl.newInstance();
            return engine;
        } catch (ClassNotFoundException e) {
            //LOG.error("instantiant com.vip.osp.route.RouteEngineImpl failed, using NoopRouteEngine", e);
        } catch (InstantiationException e) {
            LOG.error("instantiant com.vip.osp.route.RouteEngineImpl failed, using NoopRouteEngine", e);
        } catch (IllegalAccessException e) {
            LOG.error("instantiant com.vip.osp.route.RouteEngineImpl failed, using NoopRouteEngine", e);
        }
        return new NoopRouteEngine();
    }

    static class NoopRouteConfig implements RouteEngine.RouteConfig {

        @Override
        public boolean isServerMatched(InvocationContext ctx,
                                       InetAddress serverIP) {
            return true;
        }


    }

    static class NoopRouteEngine implements RouteEngine {

        @Override
        public RouteConfig parse(String routeText) {
            return new NoopRouteConfig();
        }

    }

}
