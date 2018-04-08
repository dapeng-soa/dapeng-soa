package com.github.dapeng.registry.zookeeper;

import com.github.dapeng.core.enums.LoadBalanceStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年03月22日 上午10:21
 */
public class ZkConfigInfo {

    /**
     * timeout zk config
     */
    public Config<Long> timeConfig = new Config<>();
    /**
     * loadBalance zk config
     */
    public Config<LoadBalanceStrategy> loadbalanceConfig = new Config<>();


    public static class Config<T> {
        public T globalConfig;
        public Map<String, T> serviceConfigs = new HashMap<>();
        public Map<String, T> instanceConfigs = new HashMap<>();
    }


}
