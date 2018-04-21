package com.github.dapeng.router.token;

import com.github.dapeng.core.helper.IPUtils;

/**
 * 描述: Ip 词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:08
 */
public class IpToken extends SimpleToken {

    public final int ip;
    public final int mask;

    public IpToken(int ip, int mask) {
        super(IP);
        this.ip = ip;
        this.mask = mask;
    }

    @Override
    public String toString() {
        return "IpToken[id:" + id + ", ip:" + IPUtils.transferIp(ip) + "/" + mask + "]";
    }
}
