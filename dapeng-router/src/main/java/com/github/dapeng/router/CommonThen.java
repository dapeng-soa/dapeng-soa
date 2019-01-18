package com.github.dapeng.router;

/**
 * 路由匹配成功后，导向的具体 目标
 *
 * @author huyj
 * @Created 2019-01-17 11:17
 */
public class CommonThen {
    public final boolean not;
    private final int routeType;

    public CommonThen(int routeType, boolean not) {
        this.not = not;
        this.routeType = routeType;
    }

    public boolean isNot() {
        return not;
    }

    public int getRouteType() {
        return routeType;
    }

    @Override
    public String toString() {
        return "CommonThen{" +
                "not=" + not +
                ", routeType=" + routeType +
                '}';
    }
}
