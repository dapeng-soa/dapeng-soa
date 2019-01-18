package com.github.dapeng.router;

import com.github.dapeng.router.condition.Condition;

import java.util.List;

/**
 * 描述: 代表 每一条路由表达式解析出来的内容
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:45
 */
public class Route {

    private final Condition left;
    private final List<CommonThen> thenRouteDests;

    public Route(Condition left, List<CommonThen> thenRouteDests) {
        this.left = left;
        this.thenRouteDests = thenRouteDests;
    }

    public Condition getLeft() {
        return left;
    }

    public List<CommonThen> getThenRouteDests() {
        return thenRouteDests;
    }

    @Override
    public String toString() {
        return "Route{" +
                "left=" + left +
                ", thenRouteDests=" + thenRouteDests +
                '}';
    }
}
