package com.github.dapeng.route.pattern;

/**
 *
 * @author tangliu
 * @date 2016/6/24
 */
public class IpNode {


    public IpNode(String ip, int mask) {
        this.ip = ip;
        this.mask = mask;
    }

    public final String ip;

    public final int mask;
}
