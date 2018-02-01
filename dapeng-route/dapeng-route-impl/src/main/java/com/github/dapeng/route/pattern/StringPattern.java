package com.github.dapeng.route.pattern;

/**
 * Created by tangliu on 2016/6/19.
 */
public class StringPattern extends Pattern {

    public String[] value;

    public String[] getValue() {
        return value;
    }

    public void setValue(String[] value) {
        this.value = value;
    }

    public StringPattern(String value) {
        this.value = value.split("[|]");
    }
}
