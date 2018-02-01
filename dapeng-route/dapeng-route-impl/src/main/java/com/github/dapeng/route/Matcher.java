package com.github.dapeng.route;

import com.github.dapeng.route.pattern.Pattern;

import java.util.List;

/**
 * Created by tangliu on 2016/6/19.
 */
public class Matcher {

    public Id id;

    public List<Pattern> patterns;

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<Pattern> patterns) {
        this.patterns = patterns;
    }
}
