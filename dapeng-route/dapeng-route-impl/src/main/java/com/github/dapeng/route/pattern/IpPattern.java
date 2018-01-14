package com.github.dapeng.route.pattern;

import java.util.List;

/**
 *
 * @author tangliu
 * @date 2016/6/19
 */
public class IpPattern extends Pattern {

    public final List<IpNode> ips;
    public IpPattern(List<IpNode> nodes) {
        this.ips = nodes;
    }

}
