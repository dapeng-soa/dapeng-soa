package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.impl.plugins.monitor.ServerCounterContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 链路状态控制
 * 1. 读写超时处理
 * 2. 连接数监控
 * 3. 流量监控s
 *
 * @author ever
 * @date 2018-05-29
 */
@ChannelHandler.Sharable
public class SoaLinkStateHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaLinkStateHandler.class);
    private static final ServerCounterContainer counterContainer = ServerCounterContainer.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::read");
        }
        try {
            long requestFlow = (long) ((ByteBuf) msg).readableBytes();
            counterContainer.addRequestFlow(requestFlow);
        } finally {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::write");
        }
        try {
            long responseFlow = (long) ((ByteBuf) msg).readableBytes();
            counterContainer.addResponseFlow(responseFlow);
        } finally {
            ctx.write(msg, promise);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;

            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
                LOGGER.info(getClass().getName() + "::读超时，关闭连接:" + ctx.channel());

            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(ctx.alloc().buffer(1).writeInt(0));

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(getClass().getName() + "::写超时，发送心跳包:" + ctx.channel());

            } else if (e.state() == IdleState.ALL_IDLE) {

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(getClass().getName() + "::读写都超时，发送心跳包:" + ctx.channel());
            }
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        int active = counterContainer.increaseActiveChannelAndGet();
        int total = counterContainer.increaseTotalChannelAndGet();
        LOGGER.info("新Channel连接建立:{}, 连接状态:{}/{}/{}", ctx.channel(), active, counterContainer.getInactiveChannel(), total);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        int active = counterContainer.decreaseActiveChannelAndGet();
        int inactive = counterContainer.increaseInactiveChannelAndGet();
        LOGGER.info("Channel连接关闭:{}, 连接状态:{}/{}/{}", ctx.channel(), active, inactive, counterContainer.getTotalChannel());
    }
}
