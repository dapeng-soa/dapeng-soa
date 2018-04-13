package com.github.dapeng.router.token;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:09
 */
public class ModeToken extends SimpleToken {
    public final int base;
    public final int from;
    public final int to;

    public ModeToken(int base, int from, int to) {
        super(MODE);

        this.base = base;
        this.from = from;
        this.to = to;
    }

}
