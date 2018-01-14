package com.github.dapeng.route.pattern;

/**
 *
 * @author tangliu
 * @date 2016/6/19
 */
public class RangePattern extends Pattern {

    public final long low;

    public final long high;

    public RangePattern(long low, long high) {
        this.low = low;
        this.high = high;
    }
}
