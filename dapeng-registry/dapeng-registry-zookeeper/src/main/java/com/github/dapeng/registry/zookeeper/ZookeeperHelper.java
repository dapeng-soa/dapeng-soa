package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.registry.RegistryAgent;
import com.github.dapeng.core.helper.MasterHelper;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author tangliu
 * @date 2016/2/29
 */
public class ZookeeperHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperHelper.class);

    private String zookeeperHost = SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST;

    private ZooKeeper zk;
    private RegistryAgent registryAgent;

    public ZookeeperHelper(RegistryAgent registryAgent) {
        this.registryAgent = registryAgent;
    }

    /**
     * zk 客户端实例化
     * 使用 CountDownLatch 门闩 锁，保证zk连接成功后才返回
     */
    public void connect() {
        try {
            CountDownLatch semaphore = new CountDownLatch(1);

            zk = new ZooKeeper(zookeeperHost, 15000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.Expired) {
                    LOGGER.info("ZookeeperHelper session timeout to  {} [Zookeeper]", zookeeperHost);
                    destroy();
                    connect();

                } else if (Watcher.Event.KeeperState.SyncConnected == watchedEvent.getState()) {
                    semaphore.countDown();
                    LOGGER.info("ZookeeperHelper connected to  {} [Zookeeper]", zookeeperHost);
//                    addMasterRoute();
                    if (registryAgent != null) {
                        registryAgent.registerAllServices();//重新注册服务
                    }

                } else if (Watcher.Event.KeeperState.Disconnected == watchedEvent.getState()) {
                    //zookeeper重启或zookeeper实例重新创建
                    LOGGER.error("Registry {} zookeeper 连接断开，可能是zookeeper重启或重建");

                    isMaster.clear(); //断开连接后，认为，master应该失效，避免某个孤岛一直以为自己是master

                    destroy();
                    connect();
                }
            });

            semaphore.await();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 关闭 zk 连接
     */
    public void destroy() {
        if (zk != null) {
            try {
                LOGGER.info("ZookeeperHelper closing connection to zookeeper {}", zookeeperHost);
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    /**
     * @param path /soa/runtime/services/com.github.user.UserService/192.168.1.12:9081:1.0.0
     * @param data
     */
    public void addOrUpdateServerInfo(String path, String data) {
        String[] paths = path.split("/");
        String serviceName = paths[4];
        String instancePath = paths[5];
        String versionName = instancePath.substring(instancePath.lastIndexOf(":") + 1);

        String watchPath = path.substring(0, path.lastIndexOf("/"));

        String createPath = "/";
        for (int i = 1; i < paths.length - 1; i++) {
            createPath += paths[i];
            // 异步递归创建持久化节点
            addPersistServerNodeAsync(createPath, "");
            createPath += "/";
        }


        addServerInfo(path + ":", data);
        //添加 watch ，监听子节点变化
        watchInstanceChange(watchPath, serviceName, versionName, instancePath);
    }


    /**
     * 监听服务节点下面的子节点（临时节点，实例信息）变化
     */
    private void watchInstanceChange(String path, String serviceName, String versionName, String instancePath) {
        try {
            List<String> children = zk.getChildren(path, event -> {
                //Children发生变化，则重新获取最新的services列表
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取子节点...", event.getPath());
                    // chekck
                    watchInstanceChange(path, serviceName, versionName, instancePath);
                }
            });

            checkIsMaster(children, MasterHelper.generateKey(serviceName, versionName), instancePath);

        } catch (KeeperException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            // fixme
            watchInstanceChange(path, serviceName, versionName, instancePath);
        }

    }


    /**
     * 异步添加持久化的节点
     *
     * @param path
     * @param data
     */
    private void addPersistServerNodeAsync(String path, String data) {
        Stat stat = exists(path);

        if (stat == null) {
            zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, persistNodeCreateCallback, data);
        }
    }

    private Stat exists(String path) {
        Stat stat = null;
        try {
            stat = zk.exists(path, false);
        } catch (KeeperException e) {
        } catch (InterruptedException e) {
        }
        return stat;
    }

    /**
     * 异步添加持久化节点回调方法
     */
    private AsyncCallback.StringCallback persistNodeCreateCallback = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("创建节点:{},连接断开，重新创建", path);
                addPersistServerNodeAsync(path, (String) ctx);
                break;
            case OK:
                LOGGER.info("创建节点:{},成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("创建节点:{},已存在", path);
                updateServerInfo(path, (String) ctx);
                break;
            default:
                LOGGER.info("创建节点:{},失败", path);
        }
    };


    /**
     * 异步添加serverInfo,为临时有序节点，如果server挂了就木有了
     */
    public void addServerInfo(String path, String data) {
        zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL, serverInfoCreateCallback, data);
    }

    /**
     * 异步添加serverInfo 临时节点 的回调处理
     */
    private AsyncCallback.StringCallback serverInfoCreateCallback = (rc, path, ctx, name) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                LOGGER.info("添加serviceInfo:{},连接断开，重新添加", path);

                addOrUpdateServerInfo(path, (String) ctx);
                break;
            case OK:
                LOGGER.info("添加serviceInfo:{},成功", path);
                break;
            case NODEEXISTS:
                LOGGER.info("添加serviceInfo:{},已存在，删掉后重新添加", path);
                try {
                    //只删除了当前serviceInfo的节点
                    zk.delete(path, -1);
                } catch (Exception e) {
                    LOGGER.error("删除serviceInfo:{} 失败:{}", path, e.getMessage());
                }
                addOrUpdateServerInfo(path, (String) ctx);
                break;
            default:
                LOGGER.info("添加serviceInfo:{}，出错", path);
        }
    };

    /**
     * 异步更新节点信息
     */
    private void updateServerInfo(String path, String data) {
        zk.setData(path, data.getBytes(), -1, serverInfoUpdateCallback, data);
    }

    /**
     * 异步更新节点信息的回调方法
     */
    private AsyncCallback.StatCallback serverInfoUpdateCallback = (rc, path1, ctx, stat) -> {
        switch (KeeperException.Code.get(rc)) {
            case CONNECTIONLOSS:
                updateServerInfo(path1, (String) ctx);
                return;
            default:
                //just skip
        }
    };

    public void setZookeeperHost(String zookeeperHost) {
        this.zookeeperHost = zookeeperHost;
    }

    //-----竞选master---
    private static Map<String, Boolean> isMaster = MasterHelper.isMaster;


    private static final String RUNTIME_PATH = "/soa/runtime/services/";


    /**
     * @param children    当前方法下的实例列表，        eg 127.0.0.1:9081:1.0.0,192.168.1.12:9081:1.0.0
     * @param serviceKey  当前服务信息                eg com.github.user.UserService:1.0.0
     * @param instanceKey 当前服务节点实例信息         eg  192.168.10.17:9081:1.0.0
     */
    public void checkIsMaster(List<String> children, String serviceKey, String instanceKey) {
        if (children.size() <= 0) {
            return;
        }

        /**
         * 排序规则
         * a: 192.168.100.1:9081:1.0.0-0000000022
         * b: 192.168.100.1:9081:1.0.0-0000000014
         * 根据 - 之后的数字进行排序，由小到大，每次取zk临时有序节点中的序列最小的节点作为master
         */
        Collections.sort(children, (o1, o2) -> {
            Integer int1 = Integer.valueOf(o1.substring(o1.lastIndexOf(":") + 1));
            Integer int2 = Integer.valueOf(o2.substring(o2.lastIndexOf(":") + 1));
            return int1 - int2;
        });

        String firstNode = children.get(0);

        System.out.println("------------->  " + firstNode);

        String firstInfo = firstNode.replace(firstNode.substring(firstNode.lastIndexOf(":")), "");

        System.out.println("------------->  " + firstInfo);


        if (firstInfo.equals(instanceKey)) {
            isMaster.put(serviceKey, true);
            LOGGER.info("({})竞选master成功, master({})", serviceKey, CURRENT_CONTAINER_ADDR);
        } else {
            isMaster.put(serviceKey, false);
            LOGGER.info("({})竞选master失败，当前节点为({})", serviceKey);
        }
    }


    private static final String CURRENT_CONTAINER_ADDR = SoaSystemEnvProperties.SOA_CONTAINER_IP + ":" +
            String.valueOf(SoaSystemEnvProperties.SOA_CONTAINER_PORT);


}
