package com.github.dapeng.router.token;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:08
 */
public class RangeToken extends SimpleToken {

    public final int from;
    public final int to;

    public RangeToken(int from, int to) {
        super(RANGE);
        this.from = from;
        this.to = to;
    }
}
