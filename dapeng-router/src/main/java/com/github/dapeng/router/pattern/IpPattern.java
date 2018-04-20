package com.github.dapeng.router.pattern;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:41
 */
public class IpPattern implements Pattern {
    public final String ip;
    public final int mask;

    public IpPattern(String ip, int mask) {
        this.ip = ip;
        this.mask = mask;
    }
}
