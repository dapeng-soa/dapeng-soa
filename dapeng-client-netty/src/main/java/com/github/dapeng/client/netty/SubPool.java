package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnection;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class SubPool {

    private final ReentrantLock connectionLock = new ReentrantLock();

    private final String ip;
    private final int port;

    /**
     * connection that used by rpcClients, such as java, scala, php..
     */
    private SoaConnection soaConnection;

    SubPool(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public SoaConnection getConnection() {

        if (soaConnection != null) return soaConnection;
        try {
            connectionLock.lock();
            if (soaConnection == null) {
                soaConnection = new SoaConnectionImpl(ip, port, this);
            }
        } finally {
            connectionLock.unlock();
        }
        return soaConnection;
    }

//    public void removeConnection() {
//        soaConnection = null;
//    }
}