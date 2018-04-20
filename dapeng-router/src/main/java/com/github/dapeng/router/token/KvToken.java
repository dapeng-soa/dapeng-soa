package com.github.dapeng.router.token;

import java.util.Collections;
import java.util.Map;

/**
 * 描述: key-value  词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:09
 */
public class KvToken extends SimpleToken {

    public final Map<String, String> kv;

    public KvToken(Map<String, String> kv) {
        super(KV);
        this.kv = Collections.unmodifiableMap(kv);
    }
}
