package com.github.dapeng.impl.plugins.netty;

import io.netty.util.AttributeKey;

import java.util.List;
import java.util.Map;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年03月22日 下午8:03
 */
public class NettyChannel {



    public static AttributeKey<Map<Integer, Long>> NETTY_CHANNEL_KEY = AttributeKey.valueOf("netty_channel_time");
}
