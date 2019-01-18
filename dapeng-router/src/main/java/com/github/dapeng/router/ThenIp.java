package com.github.dapeng.router;

import com.github.dapeng.core.helper.IPUtils;

/**
 * <p>
 * 路由匹配成功后，导向的具体 服务ip 实体类
 * <p>
 * etc.  method match 'setFoo' => ip'1.1.1.1'
 * 后面的ip'1.1.1.1' 包装为 ThenIp实体
 * </p>
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:42
 */
public class ThenIp extends CommonThen {
    public final int ip;
    public final int port;
    public final int mask;


    public ThenIp(int routeType, boolean not, int ip, int port, int mask) {
        super(routeType,not);
        this.ip = ip;
        this.port = port;
        this.mask = mask;
    }

    @Override
    public String toString() {
        return "ThenIp{" + "not=" + not + ", ip=" + IPUtils.transferIp(ip) + "/" + mask + ", port=" + port + '}';
    }
}
