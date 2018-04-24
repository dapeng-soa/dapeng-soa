package com.github.dapeng.impl.plugins.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author with struy.
 * Create by 2018/4/8 19:17
 * email :yq1724555319@gmail.com
 */

@ChannelHandler.Sharable
public class NettyConnectCounter extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnectCounter.class);
    private static AtomicInteger ACTIVE_CHANNEL = new AtomicInteger(0);
    private static AtomicInteger INACTIVE_CHANNEL = new AtomicInteger(0);
    private static AtomicInteger TOTAL_CHANNEL = new AtomicInteger(0);

    public static Integer getActiveChannel() {
        return ACTIVE_CHANNEL.get();
    }

    public static Integer getInactiveChannel() {
        return INACTIVE_CHANNEL.get();
    }

    public static Integer getTotalChannel() {
        return TOTAL_CHANNEL.get();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        ACTIVE_CHANNEL.incrementAndGet();
        TOTAL_CHANNEL.incrementAndGet();
        LOGGER.info("==> Channel连接建立 当前连接数:[{}]",ACTIVE_CHANNEL.get());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        ACTIVE_CHANNEL.decrementAndGet();
        INACTIVE_CHANNEL.incrementAndGet();
        LOGGER.info("==> Channel连接关闭 当前连接数:[{}]",ACTIVE_CHANNEL.get());
    }


}
