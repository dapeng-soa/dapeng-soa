package com.github.dapeng.router.pattern;

import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;

/**
 * 描述: ip 条件表达式
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:41
 */
public class IpPattern implements Pattern {
    public final int ip;
    public final int mask;

    public IpPattern(int ip, int mask) {
        this.ip = ip;
        this.mask = mask;
    }

    @Override
    public String toString() {
        return "IpPattern{" +
                "ip=" + IPUtils.transferIp(ip) +
                ", mask=" + mask +
                '}';
    }
}
