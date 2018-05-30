package com.github.dapeng.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.dapeng.common.BaseConfig.*;

/**
 * Zk 连接工厂
 *
 * @author huyj
 * @Created 2018/5/24 15:55
 */
public class ZKConnectFactory {
    private static final Logger logger = LoggerFactory.getLogger(ZKConnectFactory.class);

    //rivate static volatile CuratorFramework curatorFramework;
    private static ConcurrentHashMap<String, CuratorFramework> curatorMaps = new ConcurrentHashMap<>();
    //重试策略，初试时间1秒，重试10次
    private static RetryPolicy policy = new ExponentialBackoffRetry(RETRY_TIME, RETRY_NUM);
    private static ReentrantLock createCuratorLock = new ReentrantLock();

    public static CuratorFramework getCuratorClient(String host) {
        String connStr = host;
        if (StringUtils.isBlank(host)) connStr = CONNECT_ADDR;

        CuratorFramework curatorFramework = curatorMaps.get(connStr);
        if (curatorFramework == null) {
            try {
                createCuratorLock.lock();
                curatorFramework = curatorMaps.get(connStr);
                if (curatorFramework == null) {
                    curatorFramework = createCurator(connStr);
                    curatorMaps.put(connStr, curatorFramework);
                }
            } finally {
                createCuratorLock.unlock();
            }
        }
        return curatorFramework;
    }

    private static CuratorFramework createCurator(String host) {
        //通过工厂创建Curator
        //CuratorFramework curator = CuratorFrameworkFactory.builder().namespace(NAMESPACE).connectString(host).sessionTimeoutMs(SESSION_TIMEOUT).retryPolicy(policy).build();
        CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(host)
                .sessionTimeoutMs(SESSION_TIMEOUT_MS)
                .connectionTimeoutMs(CONNECTION_TIMEOUT_MS)
                .retryPolicy(policy)
                .build();
        //开启连接
        curator.start();
        logger.info("build zk connect on [{}]...", host);
        return curator;
    }

    public static boolean destroyCurator(String host) {
        CuratorFramework curator = curatorMaps.remove(host);
        if (curator != null) {
            curator.close();
            return true;
        } else {
            logger.info("[destroyCurator] ==> not found the zkClient connect on [{}]", host);
            return false;
        }
    }
}
