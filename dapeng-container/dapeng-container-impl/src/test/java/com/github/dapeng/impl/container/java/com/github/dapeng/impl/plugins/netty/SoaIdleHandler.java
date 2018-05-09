package com.github.dapeng.impl.plugins.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tangliu on 2016/1/14.
 */
@ChannelHandler.Sharable
public class SoaIdleHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaIdleHandler.class);

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
}
