package com.github.dapeng.router.token;

/**
 * 描述: 范围  词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:08
 */
public class RangeToken extends SimpleToken {

    public final long from;
    public final long to;

    public RangeToken(long from, long to) {
        super(RANGE);
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "RangeToken[id:" + id + ", range:" + from + ".." + to + "]";
    }
}
