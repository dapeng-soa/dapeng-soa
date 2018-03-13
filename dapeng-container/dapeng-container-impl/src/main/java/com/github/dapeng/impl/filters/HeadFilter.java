package com.github.dapeng.impl.filters;


import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.impl.plugins.netty.SoaMessageProcessor;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

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
        ByteBuf outputBuf = null;
        ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) ctx.getAttribute("channelHandlerContext");
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        BeanSerializer serializer = (BeanSerializer) ctx.getAttribute("respSerializer");
        Object result = ctx.getAttribute("result");
        SoaHeader soaHeader = context.getHeader();
        Optional<String> respCode = soaHeader.getRespCode();
        try {
            if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
                writeErrorMessage(channelHandlerContext, context, new SoaException(respCode.get(),
                        soaHeader.getRespMessage().orElse(SoaCode.UnKnown.getMsg())));
            } else if (channelHandlerContext != null) {
                outputBuf = channelHandlerContext.alloc().buffer(8192);
                TSoaTransport transport = new TSoaTransport(outputBuf);

                SoaMessageProcessor builder = new SoaMessageProcessor(transport);
                builder.writeHeader(context);
                if (serializer != null && result != null) {
                    builder.writeBody(serializer, result);
                }
                builder.writeMessageEnd();
                transport.flush();

                assert (outputBuf.refCnt() == 1);
                channelHandlerContext.writeAndFlush(outputBuf);
            } else {
                writeErrorMessage(channelHandlerContext, context, new SoaException(SoaCode.UnKnown));
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);

            writeErrorMessage(channelHandlerContext, context, new SoaException(SoaCode.UnKnown, e.getMessage()));
            if (outputBuf != null) {
                outputBuf.release();
            }
        }

    }

    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext context, SoaException e) {
        ByteBuf outputBuf = ctx.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor builder = new SoaMessageProcessor(transport);
        SoaHeader soaHeader = context.getHeader();
        try {
            soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(e.getMsg()));
            builder.writeHeader(context);
            builder.writeMessageEnd();

            transport.flush();

            ctx.writeAndFlush(outputBuf);

        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            outputBuf.release();
        }

    }
}
