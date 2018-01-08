package com.github.dapeng.route.pattern;

/**
 * Created by tangliu on 2016/6/19.
 */
public class NumberPattern extends Pattern {

    public long[] value;

    public long[] getValue() {
        return value;
    }

    public void setValue(long[] value) {
        this.value = value;
    }

    public NumberPattern(String value) {
        String[] valuesStr = value.split("[|]");
        this.value = new long[valuesStr.length];
        for (int i = 0; i < valuesStr.length; i++) {
            this.value[i] = Long.parseLong(valuesStr[i]);
        }
    }
}
