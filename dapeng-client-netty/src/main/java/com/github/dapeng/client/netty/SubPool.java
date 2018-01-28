package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnection;
import com.github.dapeng.util.CommonUtil;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class SubPool {

    private final ReentrantLock connectionLock = new ReentrantLock();
    private final ReentrantLock jsonConnectionlock = new ReentrantLock();

    private final String ip;
    private final int port;

    /**
     * connection that used by rpcClients, such as java, scala, php..
     */
    private SoaConnection normalConnection;

    /**
     * connection that used by json based httpClients.
     */
    private SoaConnection jsonConnection;

    SubPool(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * @param connectionType
     * @return
     */
    public SoaConnection getConnection(ConnectionType connectionType) {
        switch (connectionType) {
            case Json:
                if (jsonConnection != null) return jsonConnection;
                CommonUtil.newInstWithDoubleCheck(
                        jsonConnection,
                        () -> new SoaJsonConnectionImpl(ip, port),
                        jsonConnectionlock);
                return jsonConnection;
            case Common:
                if (normalConnection != null) return normalConnection;
                CommonUtil.newInstWithDoubleCheck(
                        normalConnection,
                        () -> new SoaConnectionImpl(ip, port),
                        connectionLock);
                return normalConnection;
            default:
                return null;
        }
    }
}