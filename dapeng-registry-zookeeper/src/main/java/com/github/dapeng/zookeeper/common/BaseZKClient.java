package com.github.dapeng.zookeeper.common;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.zookeeper.utils.CuratorMonitorUtils;
import com.github.dapeng.zookeeper.utils.NativeMonitorUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static com.github.dapeng.zookeeper.common.BaseConfig.ZK_ROOT_PATH;

/**
 * ZK客户端 基础封装
 *
 * @author huyj
 * @Created 2018/5/24 17:22
 */
public class BaseZKClient {
    private static Logger logger = LoggerFactory.getLogger(BaseZKClient.class);
    private static String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;
    private static boolean isNativeClient = SoaSystemEnvProperties.SOA_ZOOKEEPER_NATIVE;
    private static CuratorFramework curatorFramework;
    private static ZooKeeper zooKeeper;
    private ZkDataContext zkDataContext;

    //数据同步信号量
    private CountDownLatch sysncSemaphore = null;
    //private Semaphore sysncSemaphore = new Semaphore(1, false);


    public BaseZKClient(String host, boolean isMonitorFlag, ZK_TYPE zk_type) {
        zkHost = host;
        zkDataContext = new ZkDataContext();
        if (isNativeClient) {//是否使用原生连接
            zooKeeper = ZKConnectFactory.getZooKeeperClient(zkHost);
        } else {
            curatorFramework = ZKConnectFactory.getCuratorClient(zkHost);
        }

        if (isMonitorFlag) {
            try {
                if (isNativeClient) {
                    NativeMonitorUtils.MonitorNativeZkData(ZK_ROOT_PATH, zooKeeper, zk_type, this);
                } else {
                    CuratorMonitorUtils.MonitorCuratorZkData(ZK_ROOT_PATH, curatorFramework, zk_type, this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /*****zk 客户端常用方法*********************************************************/
    //递归创建节点
    private String recurseCreate(String path, byte[] data, CreateMode createMode) throws Exception {
        //持久节点  不能重复创建
        if (checkExists(path) && (createMode == CreateMode.PERSISTENT || createMode == CreateMode.PERSISTENT_SEQUENTIAL)) {
            return path;
        }
        int index = path.lastIndexOf("/");
        if (index > 0) {
            String parentPath = path.substring(0, index);
            //判断父节点是否存在...
            if (!checkExists(parentPath)) {
                recurseCreate(parentPath, null, CreateMode.PERSISTENT);
            }
        }
        if (Objects.isNull(createMode)) {
            return isNativeClient ? zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT) : curatorFramework.create().forPath(path, data);
        } else {
            return isNativeClient ? zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode) : curatorFramework.create().withMode(createMode).forPath(path, data);
        }
    }

    //创建普通节点
    public String create(String path, byte[] data) throws Exception {
        return recurseCreate(path, data, CreateMode.PERSISTENT);
    }

    //创建持久化节点
    public String createPersistent(String path, byte[] data) throws Exception {
        //return curatorFramework.create().withMode(CreateMode.PERSISTENT).forPath(path, data);
        return recurseCreate(path, data, CreateMode.PERSISTENT);
    }

    //创建持久化 有序 节点
    public String createPersistentSequential(String path, byte[] data) throws Exception {
        //return curatorFramework.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path, data);
        return recurseCreate(path + ":", data, CreateMode.PERSISTENT_SEQUENTIAL);
    }

    //创建临时节点
    public String createEphemeral(String path, byte[] data) throws Exception {
        // return curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(path, data);
        return recurseCreate(path, data, CreateMode.EPHEMERAL);
    }

    //创建临时 有序 节点
    public String createEphemeralSequential(String path, byte[] data) throws Exception {
        // data using Curator protection.
        //return curatorFramework.create().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path + ":", data);
        return recurseCreate(path + ":", data, CreateMode.EPHEMERAL_SEQUENTIAL);
    }


    //设置节点数据
    public Stat setData(String path, byte[] data) throws Exception {
        // set data for the given node
        return isNativeClient ? zooKeeper.setData(path, data, -1) : curatorFramework.setData().forPath(path, data);
    }

    //设置节点数据
    public byte[] getData(String path) throws Exception {
        // set data for the given node
        return isNativeClient ? zooKeeper.getData(path, true, null) : curatorFramework.getData().forPath(path);
    }

    // 删除节点
    public void delete(String path) throws Exception {
        if (isNativeClient) {
            zooKeeper.delete(path, -1);
        } else {
            curatorFramework.delete().forPath(path);
        }
    }

    // 删除节点
    private boolean checkExists(String path) throws Exception {
        if (isNativeClient) {
            return Objects.nonNull(zooKeeper.exists(path, true));
        } else {
            return Objects.nonNull(curatorFramework.checkExists().forPath(path));
        }
    }

    public void destroy() {
        ZKConnectFactory.destroyConnect(zkHost);
    }

    public void lockZkDataContext() {
        sysncSemaphore = new CountDownLatch(1);
    }

    public void releaseZkDataContext() {
        if (sysncSemaphore != null) {
            sysncSemaphore.countDown();
        }
    }

    public ZkDataContext zkDataContext() {
        return zkDataContext;
    }

    public ZkDataContext getZkDataContext() {
        while (sysncSemaphore == null) {
            logger.info("正在同步ZK数据.....");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            sysncSemaphore.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkDataContext;
    }


    /**********ZK Client type  enum**********/
    public enum ZK_TYPE {
        CLIENT, SERVER
    }
}
