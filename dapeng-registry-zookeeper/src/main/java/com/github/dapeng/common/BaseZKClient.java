package com.github.dapeng.common;

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.zoomkeeper.monitor.ZkMonitorUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import static com.github.dapeng.common.BaseConfig.ZK_ROOT_PATH;

/**
 * ZK客户端 基础封装
 *
 * @author huyj
 * @Created 2018/5/24 17:22
 */
public class BaseZKClient {
    private static Logger logger = LoggerFactory.getLogger(BaseZKClient.class);
    private static String zkHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;
    private static CuratorFramework curatorFramework;
    private ZkDataContext zkDataContext;

    //数据同步信号量
    private CountDownLatch sysncSemaphore = null;
    //private Semaphore sysncSemaphore = new Semaphore(1, false);


    public BaseZKClient(String host, boolean isMonitorFlag, ZK_TYPE zk_type) {
        zkHost = host;
        zkDataContext = new ZkDataContext();
        curatorFramework = ZKConnectFactory.getCuratorClient(zkHost);

        if (isMonitorFlag) {
            try {
                ZkMonitorUtils.MonitorZkData(ZK_ROOT_PATH, curatorFramework, zk_type, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /*****zk 客户端常用方法*********************************************************/
    //创建普通节点
    public String create(String path, byte[] data) throws Exception {
        return recurseCreate(path, data, CreateMode.PERSISTENT);
    }

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
            return curatorFramework.create().forPath(path, data);
        } else {
            return curatorFramework.create().withMode(createMode).forPath(path, data);
        }
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
        return curatorFramework.setData().forPath(path, data);
    }


    //异步 设置节点数据
    public Stat setDataAsync(String path, byte[] data) throws Exception {
        // this is one method of getting event/async notifications
        CuratorListener listener = new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
                // examine event for details
                logger.info("[eventReceived] ==> CuratorListener listener path=[{}], EventType = [{}]", event.getPath(), event.getType());
            }
        };
        curatorFramework.getCuratorListenable().addListener(listener);
        // set data for the given node asynchronously. The completion
        // notification
        // is done via the CuratorListener.
        return curatorFramework.setData().inBackground().forPath(path, data);
    }


    //异步 设置节点数据  设置回调
    public Stat setDataAsyncWithCallback(BackgroundCallback callback, String path, byte[] data) throws Exception {
        // this is another method of getting notification of an async completion
        return curatorFramework.setData().inBackground(callback).forPath(path, data);
    }

    // 删除节点
    public void delete(String path) throws Exception {
        curatorFramework.delete().forPath(path);
    }

    // 删除节点
    private boolean checkExists(String path) throws Exception {
        return Objects.nonNull(curatorFramework.checkExists().forPath(path));
    }

    //删除给定节点  并保证其完成
    public void guaranteedDelete(String path) throws Exception {
        curatorFramework.delete().guaranteed().forPath(path);
    }


    //获得自子节点  并可以触发setDataAsync中设置的CuratorListener
    public List<String> watchedGetChildren(String path) throws Exception {
        return curatorFramework.getChildren().watched().forPath(path);
    }


    //获得子节点  并设置Watcher
    public List<String> watchedGetChildren(String path, Watcher watcher) throws Exception {
        return curatorFramework.getChildren().usingWatcher(watcher).forPath(path);
    }

    /*public static void init() {
        if (curatorFramework == null) {
            ZKConnectFactory.getZkClient(zkHost);
        }
    }*/

    public void destroy() {
        ZKConnectFactory.destroyCurator(zkHost);
    }


    public void lockZkDataContext() throws InterruptedException {
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
