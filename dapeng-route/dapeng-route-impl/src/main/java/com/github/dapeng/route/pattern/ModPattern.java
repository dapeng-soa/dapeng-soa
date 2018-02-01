package com.github.dapeng.route.pattern;

/**
 *
 * @author tangliu
 * @date 2016/6/19
 */
public class ModPattern extends Pattern {

    public final int base;

    public final RangePattern remain;

    public ModPattern(int base, RangePattern remain) {
        this.base = base;
        this.remain = remain;
    }
}
