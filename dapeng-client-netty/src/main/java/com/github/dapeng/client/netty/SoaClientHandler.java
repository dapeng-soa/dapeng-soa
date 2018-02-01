package com.github.dapeng.client.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Created by tangliu on 2016/1/13.
 */
public class SoaClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaClientHandler.class);

    private CallBack callBack;

    public static interface CallBack {
        void onSuccess(ByteBuf msg) throws ExecutionException, InterruptedException;
    }

    public SoaClientHandler(CallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (callBack != null)
            try {
                callBack.onSuccess((ByteBuf) msg);
            } catch (ExecutionException e) {
                LOGGER.error(e.getMessage(),e);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(),e);
            }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }
}
