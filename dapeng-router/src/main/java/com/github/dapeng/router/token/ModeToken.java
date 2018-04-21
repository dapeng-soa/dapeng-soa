package com.github.dapeng.router.token;

import java.util.Optional;

/**
 * 描述: 取模  词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:09
 */
public class ModeToken extends SimpleToken {
    public final long base;
    public Optional<Long> from;
    public final long to;

    public ModeToken(long base, Optional<Long> from, long to) {
        super(MODE);
        this.base = base;
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "ModeToken[id:" + id + ", mode:" + base + "n+" + (from.isPresent() ? (from + ".." + to) : to) + "]";
    }
}
