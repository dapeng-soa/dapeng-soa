package com.github.dapeng.router;

/**
 * * 路由匹配成功后，导向的具体 服务ip 实体类
 * * etc.  method match 'setFoo' => v'1.0.0'
 * * 后面的v'1.0.0' 包装为 ThenVersion实体
 *
 * @author huyj
 * @Created 2019-01-17 10:18
 */
public class ThenVersion extends CommonThen {
    public final String version;

    public ThenVersion(int routeType, boolean not, String version) {
        super(routeType, not);
        this.version = version;
    }
}
