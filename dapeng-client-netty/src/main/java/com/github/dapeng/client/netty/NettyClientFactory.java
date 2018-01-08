package com.github.dapeng.client.netty;

/**
 * Created by lihuimin on 2018/1/3.
 */
public class NettyClientFactory {

    private static NettyClient nettyClient = new NettyClient();

    public static NettyClient getNettyClient() {
        return nettyClient;
    }
}
