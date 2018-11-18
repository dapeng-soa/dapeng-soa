package com.github.dapeng.core.router;

import java.util.List;

/**
 * 描述: 代表 每一条路由表达式解析出来的内容
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:45
 */
public class Route {

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
