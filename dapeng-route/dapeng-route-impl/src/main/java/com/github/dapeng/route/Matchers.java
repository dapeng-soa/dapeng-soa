package com.github.dapeng.route;

import java.util.List;

/**
 * Created by tangliu on 2016/6/19.
 */
public class Matchers extends MatchLeftSide {

    public List<Matcher> getMatchers() {
        return matchers;
    }

    public void setMatchers(List<Matcher> matchers) {
        this.matchers = matchers;
    }

    public boolean isAndOrOr() {
        return andOrOr;
    }

    public void setAndOrOr(boolean andOrOr) {
        this.andOrOr = andOrOr;
    }
}
