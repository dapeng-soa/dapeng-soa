package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnection;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class SubPool {

    static final int MAX = SoaSystemEnvProperties.SOA_SUBPOOL_SIZE;

    private final ReentrantLock connectionLock = new ReentrantLock();

    private final String ip;
    private final int port;

    /**
     * connection that used by rpcClients, such as java, scala, php..
     */
    private SoaConnection[] soaConnections;
    private AtomicInteger index = new AtomicInteger(0);

    SubPool(String ip, int port) {
        this.ip = ip;
        this.port = port;

        soaConnections = new SoaConnection[MAX];
        for(int i = 0; i < MAX; i++) {
            soaConnections[i] = createConnection();
        }
    }

    private SoaConnection createConnection(){
        try {
            connectionLock.lock();
            return new SoaConnectionImpl(ip, port, this);
        } finally {
            connectionLock.unlock();
        }
    }

    public SoaConnection getConnection() {
        int index = this.index.getAndIncrement();
        if(index < 0) {
            synchronized (this){
                this.index.set(0);
                index = 0;
            }
        }
        return soaConnections[index%MAX];
    }

//    public void removeConnection() {
//        soaConnection = null;
//    }
}