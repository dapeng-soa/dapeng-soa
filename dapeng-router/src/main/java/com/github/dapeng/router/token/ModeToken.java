package com.github.dapeng.router.token;

import java.util.Optional;

/**
 * 描述:
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

}
