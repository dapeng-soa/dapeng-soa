package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnection;
import com.github.dapeng.util.SoaJsonConnectionImpl;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by lihuimin on 2017/12/25.
 */
public class SubPool {

    private final ReentrantLock connectionLock = new ReentrantLock();
    private final ReentrantLock jsonConnectionlock = new ReentrantLock();

    final String ip;
    final int port;

    private SoaConnection commonConnection;

    private SoaConnection jsonConnection;

    public SubPool(String ip, int port ) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * TODO: 同一个IPPort 是否会同时存在 SoaJsonConnection & SoaConnection
     * @param connectionType
     * @return
     */
    public SoaConnection getConnection(ConnectionType connectionType) {

        if (connectionType == ConnectionType.Json) {
            try {
                connectionLock.lock();
                if (jsonConnection == null){
                    jsonConnection = new SoaJsonConnectionImpl(ip,port);
                }
                return jsonConnection;
            } finally {
                connectionLock.unlock();
            }
        } else {
            try {
                jsonConnectionlock.lock();
                if (commonConnection == null) {
                    commonConnection = new SoaConnectionImpl(ip,port);
                }
                return commonConnection;
            } finally {
                jsonConnectionlock.unlock();
            }
        }


    }
}