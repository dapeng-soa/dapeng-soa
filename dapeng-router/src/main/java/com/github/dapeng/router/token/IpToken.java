package com.github.dapeng.router.token;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:08
 */
public class IpToken extends SimpleToken {

    public final String ip;
    public final int mask;

    public IpToken(String ip, int mask) {
        super(IP);
        this.ip = ip;
        this.mask = mask;
    }
}
