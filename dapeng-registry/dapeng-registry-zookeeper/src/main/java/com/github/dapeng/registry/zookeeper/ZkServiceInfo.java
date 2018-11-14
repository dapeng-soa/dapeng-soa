package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.Weight;
import com.github.dapeng.core.enums.LoadBalanceStrategy;
import com.github.dapeng.registry.zookeeper.watcher.ZkWatcher;
import org.apache.zookeeper.Watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lihuimin
 * @date 2017/12/25
 */
public class ZkServiceInfo {

    private Watcher watcher = new ZkWatcher(this);

    /**
     * <pre>
     *  状态变迁
     *  1. ZkServiceInfo被创建，初始状态: CREATED
     *  2. 某服务客户端第一次被创建的时候，会去同步对应服务在zk上的信息，同步完成后，ZkServiceInfo状态设置为SYNCED.
     *     在该状态下， 本地信息跟zk上的信息保持同步，始终一致。
     *  3. 该服务客户端给gc后，ZkServiceInfo进入TRANSIENT状态。注意这时候本地的服务信息跟zk的服务信息还是一致的。
     *  4. ZkServiceInfo进入TRANSIENT状态后，会有2种情况:
     *    4.1 zk上该服务信息发生变化(例如服务重启引起节点数目变化)，这时候watch最后一次给唤醒，把ZkServiceInfo状态变为OUT_OF_SYNC，
     *        表示本地服务信息跟zk服务信息不一致了
     *    4.2 zk上改服务信息发生变化前，该服务又一个新的客户端给创建， 这时候ZkServiceInfo状态重新变为SYNCED.
     *  5. 该服务的新客户端给创建时，ZkServiceInfo已经在步骤1中给创建，判断其状态:
     *    5.1 如果对应的ZkServiceInfo状态为SYNCED，那么不需要对ZkServiceInfo做任何操作。
     *    5.2 如果对应的ZkServiceInfo状态为TRANSIENT，那么把ZkServiceInfo状态改为SYNCED，但不需要同步远端zk信息(因为此时本地跟zk信息还是一致的)
     *    5.3 如果对应的ZkServiceInfo状态为OUT_OF_SYNC, 那么需要去同步对应服务在zk上的信息，同步完成后，ZkServiceInfo状态设置为SYNCED
     *  SYNCED以及TRANSIENT两种状态下，本地服务信息跟zk的服务信息皆一致。
     * </pre>
     */
    public enum Status {

        CREATED, SYNCED,
        /**
         * 暂态
         */
        TRANSIENT, OUT_OF_SYNC,
    }

    private final String service;

    private Status status = Status.CREATED;

    /**
     * instances list, always use cow collection
     */
    private List<RuntimeInstance> runtimeInstances;

    public ZkServiceInfo(String service) {
        this.service = service;
    }

    public ZkServiceInfo(String service, List<RuntimeInstance> runtimeInstances) {
        this.service = service;
        this.runtimeInstances = runtimeInstances;
    }

    public List<RuntimeInstance> getRuntimeInstances() {
        return runtimeInstances;
    }

    public String getService() {
        return service;
    }

    public Status getStatus() {
        return status;
    }

    public Watcher getWatcher() {
        return watcher;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    //                           that's begin  config                              ～
    //                                                                             ～
    //～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～～
    /**
     * processTime zk config
     */
    public Config<Long> processTimeConfig = new Config<>();

    /**
     * timeout zk config
     */
    public Config<Long> timeConfig = new Config<>();
    /**
     * loadBalance zk config
     */
    public Config<LoadBalanceStrategy> loadbalanceConfig = new Config<>();

    /**
     * weight zk config
     */
    public Weight weightGlobalConfig = new Weight();

    public List<Weight> weightServiceConfigs = new ArrayList<>();

    /**
     * config class
     */
    public static class Config<T> {
        public T globalConfig;
        public Map<String, T> serviceConfigs = new HashMap<>();
        public Map<String, T> instanceConfigs = new HashMap<>();
    }
}
