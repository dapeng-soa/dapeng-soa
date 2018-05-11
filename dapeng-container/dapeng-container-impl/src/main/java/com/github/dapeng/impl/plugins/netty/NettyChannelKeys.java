package com.github.dapeng.impl.plugins.netty;

import io.netty.util.AttributeKey;

import java.util.Map;

/**
 * 描述: NettyChannelKeys 作为 handler间共享数据
 *
 * @author hz.lei
 * @date 2018年03月22日 下午8:03
 */
public class NettyChannelKeys {

    /**
     * 用于记录请求进来的时间戳
     */
    public static final AttributeKey<Map<Integer, Long>> REQUEST_TIMESTAMP = AttributeKey.valueOf("dapeng_request_timestamp");


}
