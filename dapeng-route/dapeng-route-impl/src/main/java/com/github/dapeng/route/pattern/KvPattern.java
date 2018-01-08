package com.github.dapeng.route.pattern;

import java.util.Map;

/**
 * Created by tangliu on 2016/6/19.
 */
public class KvPattern extends Pattern {

    public Map<String, String> kv;

    public Map<String, String> getKv() {
        return kv;
    }

    public void setKv(Map<String, String> kv) {
        this.kv = kv;
    }
}
