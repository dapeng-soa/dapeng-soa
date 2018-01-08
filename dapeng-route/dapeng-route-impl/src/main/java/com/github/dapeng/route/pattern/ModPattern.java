package com.github.dapeng.route.pattern;

/**
 * Created by tangliu on 2016/6/19.
 */
public class ModPattern extends Pattern {

    public int base;

    public RangePattern remain;

    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }

    public RangePattern getRemain() {
        return remain;
    }

    public void setRemain(RangePattern remain) {
        this.remain = remain;
    }

    public ModPattern(int base, RangePattern remain) {
        this.base = base;
        this.remain = remain;
    }
}
