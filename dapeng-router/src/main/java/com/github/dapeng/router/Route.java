package com.github.dapeng.router;

import com.github.dapeng.router.condition.Condition;

import java.util.List;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:45
 */
public class Route {

    private Condition left;
    private List<ThenIp> actions;

    public Route(Condition left, List<ThenIp> actions) {
        this.left = left;
        this.actions = actions;
    }

    public Condition getLeft() {
        return left;
    }

    public List<ThenIp> getActions() {
        return actions;
    }
}
