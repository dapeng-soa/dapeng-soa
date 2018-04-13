package com.github.dapeng.router;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:42
 */
public class ThenIp {

    public final boolean not;
    public final String ip;
    public final int mask;

    ThenIp(boolean not, String ip, int mask) {
        this.not = not;
        this.ip = ip;
        this.mask = mask;
    }
}
