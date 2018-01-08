package com.github.dapeng.route.pattern;

import java.util.List;

/**
 * Created by tangliu on 2016/6/19.
 */
public class IpPattern extends Pattern {

    public IpPattern() {

    }

    public IpPattern(List<IpNode> nodes) {
        this.ips = nodes;
    }

    public List<IpNode> ips;

    public List<IpNode> getIps() {
        return ips;
    }

    public void setIps(List<IpNode> ips) {
        this.ips = ips;
    }
}
