package com.github.dapeng.route.pattern;

/**
 *
 * @author tangliu
 * @date 2016/6/19
 */
public class NumberPattern extends Pattern {

    public final long[] value;

    public NumberPattern(String value) {
        String[] valuesStr = value.split("[|]");
        this.value = new long[valuesStr.length];
        for (int i = 0; i < valuesStr.length; i++) {
            this.value[i] = Long.parseLong(valuesStr[i]);
        }
    }
}
