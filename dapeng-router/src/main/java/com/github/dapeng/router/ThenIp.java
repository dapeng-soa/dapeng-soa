package com.github.dapeng.router;

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
public class ThenIp {
    public final boolean not;
    public final int ip;
    public final int mask;

    ThenIp(boolean not, int ip, int mask) {
        this.not = not;
        this.ip = ip;
        this.mask = mask;
    }

    @Override
    public String toString() {
        return "ThenIp{" +
                "not=" + not +
                ", ip=" + ip +
                ", mask=" + mask +
                '}';
    }
}
