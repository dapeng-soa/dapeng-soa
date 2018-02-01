package com.github.dapeng.route;


import com.github.dapeng.core.InvocationContext;

import java.net.InetAddress;
import java.util.List;

/**
 * @author Eric on
 * @date
 */
public class RouteConfigImpl implements RouteEngine.RouteConfig {

    List<Route> routes;

    @Override
    public boolean isServerMatched(InvocationContext ctx, InetAddress serverIP) {
        return RouteExecutor.isServerMatched(ctx, routes, serverIP);
    }
}
