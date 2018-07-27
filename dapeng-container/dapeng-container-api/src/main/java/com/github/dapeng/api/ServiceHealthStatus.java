package com.github.dapeng.api;

/**
 * 服务健康度枚举
 *
 * @author Ever
 * @date 2018/07/25
 */
public enum ServiceHealthStatus {
    /**
     * 良好
     */
    Green,
    /**
     * 一般, 业务还能进行, 但要引起注意
     */
    Yellow,
    /**
     * 业务主流程受阻
     */
    Red
}
