package com.github.dapeng.route;

import com.github.dapeng.route.pattern.Pattern;

/**
 * Created by tangliu on 2016/6/19.
 */
public class Route {

    private MatchLeftSide left;

    private Pattern right;

    public MatchLeftSide getLeft() {
        return left;
    }

    public Pattern getRight() {
        return right;
    }

    public void setLeft(MatchLeftSide left) {
        this.left = left;
    }

    public void setRight(Pattern right) {
        this.right = right;
    }
}
