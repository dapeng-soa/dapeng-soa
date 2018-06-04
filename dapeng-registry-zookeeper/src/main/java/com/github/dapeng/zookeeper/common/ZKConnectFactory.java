package com.github.dapeng.zookeeper.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.dapeng.zookeeper.common.BaseConfig.*;

/**
 * Zk 连接工厂
 *
 * @author huyj
 * @Created 2018/5/24 15:55
 */
public class ZKConnectFactory {
    private static final Logger logger = LoggerFactory.getLogger(ZKConnectFactory.class);

    //rivate static volatile CuratorFramework curatorFramework;
    private static ConcurrentHashMap<String, CuratorFramework> curatorMap = new ConcurrentHashMap<>();
    //重试策略，初试时间1秒，重试10次
    private static RetryPolicy policy = new ExponentialBackoffRetry(RETRY_TIME, RETRY_NUM);
    private static ReentrantLock createClientLock = new ReentrantLock();

    private static ConcurrentHashMap<String, ZooKeeper> zooKeeperMap = new ConcurrentHashMap<>();

    /************ appache curator ********************************************/
    public static CuratorFramework getCuratorClient(String host) {
        String connStr = host;
        if (StringUtils.isBlank(host)) connStr = CONNECT_ADDR;

        CuratorFramework curatorFramework = curatorMap.get(connStr);
        if (curatorFramework == null) {
            try {
                createClientLock.lock();
                curatorFramework = curatorMap.get(connStr);
                if (curatorFramework == null) {
                    curatorFramework = createCuratorClient(connStr);
                    curatorMap.put(connStr, curatorFramework);
                }
            } finally {
                createClientLock.unlock();
            }
        }
        return curatorFramework;
    }

    private static CuratorFramework createCuratorClient(String host) {
        //通过工厂创建Curator
        //CuratorFramework curator = CuratorFrameworkFactory.builder().namespace(NAMESPACE).connectString(host).sessionTimeoutMs(SESSION_TIMEOUT).retryPolicy(policy).build();
        CuratorFramework curator = CuratorFrameworkFactory.builder().connectString(host)
                .sessionTimeoutMs(SESSION_TIMEOUT_MS)
                .connectionTimeoutMs(CONNECTION_TIMEOUT_MS)
                .retryPolicy(policy)
                .build();
        //开启连接
        curator.start();
        logger.info("build curator zk connect on [{}]...", host);
        return curator;
    }


    /**********zookeeper native************************/
    public static ZooKeeper getZooKeeperClient(String host) {
        String connStr = host;
        if (StringUtils.isBlank(host)) connStr = CONNECT_ADDR;
        ZooKeeper zooKeeper = zooKeeperMap.get(connStr);
        if (zooKeeper == null) {
            try {
                createClientLock.lock();
                zooKeeper = zooKeeperMap.get(connStr);
                if (zooKeeper == null) {
                    zooKeeper = createZooKeeperClient(connStr);
                    zooKeeperMap.put(connStr, zooKeeper);
                }
            } finally {
                createClientLock.unlock();
            }
        }
        return zooKeeper;
    }

    private static ZooKeeper createZooKeeperClient(String host) {
        CountDownLatch semaphore = new CountDownLatch(1);
        try {
            /**
             * ZooKeeper客户端和服务器会话的建立是一个异步的过程
             * 构造函数在处理完客户端的初始化工作后立即返回，在大多数情况下，并没有真正地建立好会话
             * 当会话真正创建完毕后，Zookeeper服务器会向客户端发送一个事件通知
             */
            ZooKeeper zkClient = new ZooKeeper(host, SESSION_TIMEOUT_MS, (event) -> {
                //System.out.println("回调watcher实例： 路径" + event.getPath() + " 类型：" + event.getType());
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    semaphore.countDown();
                }
            });
            logger.info("build zk connect state1[{}]...", zkClient.getState());
            semaphore.await();
            logger.info("build zk connect state2[{}]...", zkClient.getState());
            logger.info("build native zk connect on [{}]...", host);
            return zkClient;
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param host
     * @return
     */
    public static boolean destroyConnect(String host) {
        CuratorFramework curator = curatorMap.remove(host);
        ZooKeeper zooKeeper = zooKeeperMap.remove(host);
        if (curator != null) {
            curator.close();
        } else {
            logger.info("[destroyCurator] ==> not found the zkClient connect on [{}]", host);
        }


        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            logger.info("[destroyCurator] ==> not found the zkClient connect on [{}]", host);
        }
        return true;
    }
}
