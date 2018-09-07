package com.github.dapeng.client.netty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huyj
 * @Created 2018-08-21 16:40
 */
public class SubPoolFactory {

    private final static ConcurrentHashMap<IpPort, SubPool> subPoolsMap = new ConcurrentHashMap(16);
    private static final ReentrantLock subPoolLock = new ReentrantLock();

    public static SubPool getSubPool(String ip, int port) {
        IpPort ipPort = new IpPort(ip, port);
        SubPool subPool = subPoolsMap.get(ipPort);
        if (subPool == null) {
            try {
                subPoolLock.lock();
                subPool = subPoolsMap.get(ipPort);
                if (subPool == null) {
                    subPool = new SubPool(ip, port);
                    subPoolsMap.put(ipPort, subPool);
                }
            } finally {
                subPoolLock.unlock();
            }
        }
        return subPool;
    }
}
