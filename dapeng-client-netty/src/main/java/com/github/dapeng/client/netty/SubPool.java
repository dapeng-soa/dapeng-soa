package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnection;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class SubPool {

    static final int MAX = 8;

    private final ReentrantLock connectionLock = new ReentrantLock();

    private final String ip;
    private final int port;

    /**
     * connection that used by rpcClients, such as java, scala, php..
     */
    private final SoaConnection[] soaConnections;
    private final AtomicInteger index = new AtomicInteger(0);

    SubPool(String ip, int port) {
        this.ip = ip;
        this.port = port;

        soaConnections = new SoaConnection[MAX];
        for(int i = 0; i < MAX; i++) {
            soaConnections[i] = new SoaConnectionImpl(ip, port);
        }
    }

    public SoaConnection getConnection() {
        if (MAX == 1) {
            return soaConnections[0];
        }

        int idx = this.index.getAndIncrement();
        if(idx < 0) {
            synchronized (this){
                this.index.set(0);
                idx = 0;
            }
        }
        return soaConnections[idx%MAX];
    }

//    public void removeConnection() {
//        soaConnection = null;
//    }
}