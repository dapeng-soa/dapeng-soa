package com.github.dapeng.zoomkeeper.client;

import com.github.dapeng.common.BaseZKClient;
import com.github.dapeng.common.ConfigKey;
import com.github.dapeng.common.ZkConfig;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 客户端 ZK
 *
 * @author huyj
 * @Created 2018/5/24 22:10
 */
public class ClientZk extends BaseZKClient {
    private static final Logger logger = LoggerFactory.getLogger(ClientZk.class);

    public ClientZk(String zkHost) {
        super(zkHost, true, ZK_TYPE.CLIENT);
    }

    /**
     * 获得路由规则
     *
     * @param serviceName
     * @return
     */
    public List<Route> getRoutes(String serviceName) {
        return this.getZkDataContext().getRoutesMap().get(serviceName);
    }


    /**
     * 获得 服务运行实例
     *
     * @param serviceName
     * @return
     */
    public List<RuntimeInstance> getRuntimeInstances(String serviceName) {
        return this.getZkDataContext().getRuntimeInstancesMap().get(serviceName);
    }


    /**
     * 获得 服务配置
     *
     * @param serviceName  服务名
     * @param configKey    配置枚举  @see com.github.dapeng.common.ConfigKey
     * @param method       需要拿方法级别的配置则传入方法名
     * @param defaultValue 默认值
     * @return
     */
    public Object getServiceConfig(String serviceName, ConfigKey configKey, String method, Object defaultValue) {

        HashMap<ConfigKey, ZkConfig> configHashMap = this.getZkDataContext().getConfigsMap().get(serviceName);
        if (Objects.nonNull(configHashMap) && Objects.nonNull(configHashMap.get(configKey))) {
            return this.getZkDataContext().getConfigsMap().get(serviceName).get(configKey).getConfig(configKey.getValue(), method, defaultValue);
        } else {
            return defaultValue;
        }
    }
}
