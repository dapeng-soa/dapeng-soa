package com.github.dapeng.zookeeper.common;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.zookeeper.utils.CuratorMonitorUtils;
import com.github.dapeng.zookeeper.utils.DataParseUtils;
import com.github.dapeng.zookeeper.utils.NativeMonitorUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static com.github.dapeng.zookeeper.common.BaseConfig.ZK_ROOT_PATH;

;

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
    private CLIENT_TYPE clientType;

    //数据同步信号量
    private CountDownLatch sysncSemaphore = null;
    //private Semaphore sysncSemaphore = new Semaphore(1, false);


    public BaseZKClient(String host, boolean isMonitorFlag, CLIENT_TYPE client_type) {
        clientType = client_type;
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
                    NativeMonitorUtils.MonitorNativeZkData(ZK_ROOT_PATH, zooKeeper, this);
                } else {
                    CuratorMonitorUtils.MonitorCuratorZkData(ZK_ROOT_PATH, curatorFramework, this);
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

        createMode = Objects.isNull(createMode) ? CreateMode.PERSISTENT : createMode;
        if (isNativeClient) {
            zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode, createNodeCallback, data);
        } else {
            curatorFramework.create().withMode(createMode).forPath(path, data);
        }
        return path;
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

    //获取节点数据
    public String getData(String path) {
        try {
            // set data for the given node
            if (isNativeClient ? Objects.isNull(zooKeeper.exists(path, false)) : Objects.isNull(curatorFramework.checkExists().forPath(path))) {
                return "";
            }
            byte[] data = isNativeClient ? zooKeeper.getData(path, false, null) : curatorFramework.getData().forPath(path);
            if (Objects.nonNull(data)) {
                return new String(data, "utf-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // 删除节点
    public void delete(String path) throws Exception {
        if (isNativeClient) {
            zooKeeper.delete(path, -1);
        } else {
            curatorFramework.delete().forPath(path);
        }
    }

    // 检查节点是否存在
    private boolean checkExists(String path) throws Exception {
        if (isNativeClient) {
            return Objects.nonNull(zooKeeper.exists(path, true));
           /* System.out.println("位置：BaseZKClient.checkExists ==> ***********"+path);
            return Objects.nonNull(zooKeeper.exists(path, watchedEvent -> {
                logger.info(" exists  节点 [{}] 发生变化[{}]，正在同步信息....", watchedEvent.getPath(), watchedEvent.getType());
                DataParseUtils.MonitorType eventType = DataParseUtils.getChangeEventType(true, null, watchedEvent.getType());
            }));*/
        } else {
            return Objects.nonNull(curatorFramework.checkExists().forPath(path));
        }
    }


    /**
     * 异步添加持久化节点回调方法
     */
    private AsyncCallback.StringCallback createNodeCallback = (rc, targetPath, ctx, createPath) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                logger.info("创建节点:{},连接断开，重新创建", createPath);
                break;
            case OK:
                logger.info("创建节点:{},成功", createPath);
                //NativeMonitorUtils.parseZkData(zooKeeper, createPath, this, DataParseUtils.MonitorType.TYPE_ADDED, type);
                NativeMonitorUtils.registerZkWatcher(createPath, zooKeeper, DataParseUtils.MonitorType.TYPE_ADDED, this);
                break;
            case NODEEXISTS:
                logger.info("创建节点:{},已存在", createPath);
                break;
            default:
                logger.info("创建节点:{},失败", createPath);
        }
    };


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

    public CLIENT_TYPE getClientType() {
        return clientType;
    }

    /**********ZK Client Type **********/
    public enum CLIENT_TYPE {
        CLIENT, SERVER
    }
}
