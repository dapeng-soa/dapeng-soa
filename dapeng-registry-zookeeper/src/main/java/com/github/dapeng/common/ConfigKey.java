package com.github.dapeng.common;

/**
 * ZK 配置定义
 *
 * @author huyj
 * @Created 2018/5/25 16:04
 */
public enum ConfigKey {

    Thread("thread"),
    ThreadPool("threadPool"),
    ClientTimeout("clientTimeout"),
    ServerTimeout("serverTimeout"),
    LoadBalance("loadBalance"),
    FailOver("failover"),
    Compatible("compatible"),
    SlowServiceTime("slowServiceTime"),
    TimeOut("timeout");

    private final String value;

    ConfigKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    //根据枚举值 取得枚举对象
    public static ConfigKey getConfigKeyByValue(String value) {
        for (ConfigKey configKey : ConfigKey.values()) {
            if (value.equals(configKey.value)) {
                return configKey;
            }
        }
        return null;
    }
}
