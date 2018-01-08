package com.github.dapeng.route.pattern;

/**
 * Created by tangliu on 2016/6/19.
 */
public class RangePattern extends Pattern {

    public long low;

    public long high;


    public long getLow() {
        return low;
    }

    public void setLow(long low) {
        this.low = low;
    }

    public long getHigh() {
        return high;
    }

    public void setHigh(long high) {
        this.high = high;
    }

    public RangePattern(long low, long high) {
        this.low = low;
        this.high = high;
    }
}
