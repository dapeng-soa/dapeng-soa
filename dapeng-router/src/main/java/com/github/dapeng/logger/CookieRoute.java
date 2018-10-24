package com.github.dapeng.logger;

import com.github.dapeng.router.ThenIp;
import com.github.dapeng.router.condition.Condition;

import java.util.List;

/**
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 3:52 PM
 */
public class CookieRoute {

    private Condition left;
    private List<ThenIp> thenRouteIps;

    public Route(Condition left, List<ThenIp> thenRouteIps) {
        this.left = left;
        this.thenRouteIps = thenRouteIps;
    }

    public Condition getLeft() {
        return left;
    }

    public List<ThenIp> getThenRouteIps() {
        return thenRouteIps;
    }

    @Override
    public String toString() {
        return "Route{" +
                "left=" + left +
                ", thenRouteIps:" + thenRouteIps +
                '}';
    }
}
