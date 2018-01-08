package com.github.dapeng.route.pattern;

/**
 * Created by tangliu on 2016/6/24.
 */
public class IpNode {


    public IpNode(String ip, int mask) {
        this.ip = ip;
        this.mask = mask;
    }

    String ip;

    public int mask;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }
}
