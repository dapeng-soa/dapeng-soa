package com.github.dapeng.impl.filters;


import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeadFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadFilter.class);

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) {

        try {
            next.onEntry(ctx);
        } catch (TException e) {
            LOGGER.error(e.getMessage(), e);
        }


    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) {
        // 第一个filter不需要调onExit
        ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) ctx.getAttribute("channelHandlerContext");
        channelHandlerContext.writeAndFlush(ctx);
    }
}
