package com.github.dapeng.client.netty;

public enum ConnectionType {
    /**
     * 一般的服务连接类型
     */
    Common,

    /**
     * Json类型的连接
     */
    Json;

    public static ConnectionType findByValue(String value) {
        switch (value) {
            case "Common":
                return Common;
            case "Json":
                return Json;
            default:
                return Common;
        }
    }
}