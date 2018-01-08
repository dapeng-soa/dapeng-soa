package com.github.dapeng.route.pattern;

/**
 * Created by tangliu on 2016/6/19.
 */
public class NotPattern extends Pattern {

    public Pattern pattern;

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public NotPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
