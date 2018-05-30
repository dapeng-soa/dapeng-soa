package com.github.dapeng.zoomkeeper.client;

import com.github.dapeng.common.BaseZKClient;
import com.github.dapeng.common.ConfigKey;
import com.github.dapeng.core.FreqControlRule;

import java.util.List;

/**
 * 服务端ZK
 *
 * @author huyj
 * @Created 2018/5/24 22:15
 */
public class ServerZk extends BaseZKClient {
    public ServerZk(String zkHost) {
        super(zkHost, true, ZK_TYPE.SERVER);
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
        return this.getZkDataContext().getConfigsMap().get(serviceName).get(configKey).getConfig(configKey.getValue(), method, defaultValue);
    }


    /**
     * 获取限流规则
     *
     * @param serviceKey
     * @return
     * @see com.github.dapeng.impl.plugins.netty
     */
    public List<FreqControlRule> getFreqControlRule(String serviceName) {
        return this.getZkDataContext().getFreqRulesMap().get(serviceName);
    }
}