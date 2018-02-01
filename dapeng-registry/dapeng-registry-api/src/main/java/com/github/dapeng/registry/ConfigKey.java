package com.github.dapeng.registry;

/**
 * Created by tangliu on 2016/2/16.
 */
public enum ConfigKey {

    Thread("thread"),

    ThreadPool("threadPool"),

    ClientTimeout("clientTimeout"),

    ServerTimeout("serverTimeout"),

    LoadBalance("loadBalance"),

    FailOver("failover"),

    Compatible("compatible");

    private final String value;

    private ConfigKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static ConfigKey findByValue(String value) {
        switch (value) {
            case "thread":
                return Thread;
            case "threadPool":
                return ThreadPool;
            case "clientTimeout":
                return ClientTimeout;
            case "serverTimeout":
                return ServerTimeout;
            case "loadBalance":
                return LoadBalance;
            case "failover":
                return FailOver;
            case "compatible":
                return Compatible;
            default:
                return null;
        }
    }
}
