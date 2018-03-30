package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnection;

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
     * TODO unionfy with normalConnection
     */
    private SoaConnection jsonConnection;

    //todo
    // removeConnection

    SubPool(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * @param connectionType
     * @return
     */
    public SoaConnection  getConnection(ConnectionType connectionType) {
        switch (connectionType) {
            case Json:
                if (jsonConnection != null) return jsonConnection;
                try {
                    jsonConnectionlock.lock();
                    if (jsonConnection == null) {
                        jsonConnection = new SoaJsonConnectionImpl(ip, port);
                    }
                } finally {
                    jsonConnectionlock.unlock();
                }

                return jsonConnection;
            case Common:
                if (normalConnection != null) return normalConnection;
                try {
                    connectionLock.lock();
                    if (normalConnection == null) {
                        normalConnection = new SoaConnectionImpl(ip, port);
                    }
                } finally {
                    connectionLock.unlock();
                }
                return normalConnection;
            default:
                return null;
        }
    }
}