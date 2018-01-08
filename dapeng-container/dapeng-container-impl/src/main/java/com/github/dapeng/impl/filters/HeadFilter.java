package com.github.dapeng.impl.filters;


import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.impl.plugins.netty.SoaMessageProcessor;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class HeadFilter implements Filter {

    @Override
    public void onEntry(FilterContext ctx, FilterChain next)  {

        try {
            next.onEntry(ctx);
        } catch (TException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev)  {
        // 第一个filter不需要调onExit
        try {
            ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) ctx.getAttach( "channelHandlerContext");
            TransactionContext context = (TransactionContext) ctx.getAttach("context");
            BeanSerializer serializer = (BeanSerializer) ctx.getAttach("respSerializer");
            Object result = ctx.getAttach("result");

            if(channelHandlerContext!=null) {
                ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);  // TODO 8192?
                TSoaTransport transport = new TSoaTransport(outputBuf);

                SoaMessageProcessor builder = new SoaMessageProcessor(transport);
                builder.writeHeader(context);
                if(serializer != null && result != null) {
                    builder.writeBody(serializer, result);
                }
                builder.writeMessageEnd();
                transport.flush();
                channelHandlerContext.writeAndFlush(outputBuf);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
