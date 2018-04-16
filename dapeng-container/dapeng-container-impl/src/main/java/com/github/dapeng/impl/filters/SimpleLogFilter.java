package com.github.dapeng.impl.filters;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.ContainerFilter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLogFilter implements ContainerFilter{

    private static final Logger SIMPLE_LOGGER = LoggerFactory.getLogger("container.simple.log");

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        next.onEntry(ctx);
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

        try {

            ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) ctx.getAttribute("channelHandlerContext");
            TransactionContext context = (TransactionContext) ctx.getAttribute("context");
            SoaHeader soaHeader = context.getHeader();
            long waitingTime = (long) ctx.getAttribute("waitingTime");
            long startTime = (long) ctx.getAttribute("startTime");
            Integer requestFlow = (Integer) ctx.getAttribute("requestFlow");

            StringBuilder builder = new StringBuilder("DONE")
                    .append(" ").append(channelHandlerContext.channel().remoteAddress())
                    .append(" ").append(channelHandlerContext.channel().localAddress())
                    .append(" ").append(context.getSeqid())
                    .append(" ").append(soaHeader.getServiceName()).append(".").append(soaHeader.getMethodName()).append(":").append(soaHeader.getVersionName())
                    .append(" ").append(soaHeader.getRespCode())
                    .append(" ").append(soaHeader.getRespMessage())
                    .append(" ").append(requestFlow)
                    .append(" ").append(0)
                    .append(" ").append(waitingTime).append("ms")
                    .append(" ").append(System.currentTimeMillis() - startTime).append("ms");

            SIMPLE_LOGGER.info(builder.toString());
        }catch (Exception e){
            SIMPLE_LOGGER.error(e.getMessage(),e);
        }

        prev.onExit(ctx);

    }
}
