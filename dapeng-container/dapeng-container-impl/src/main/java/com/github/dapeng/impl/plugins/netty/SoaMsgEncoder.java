package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.transport.TTransport;
import com.github.dapeng.util.SoaMessageBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

public class SoaMsgEncoder extends MessageToByteEncoder<FilterContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaMsgDecoder.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, FilterContext ctx, ByteBuf out) throws Exception {
        ByteBuf outputBuf;
        TransactionContext context = (TransactionContext) ctx.getAttribute("context");
        BeanSerializer serializer = (BeanSerializer) ctx.getAttribute("respSerializer");
        Object result = ctx.getAttribute("result");
        SoaHeader soaHeader = context.getHeader();
        Optional<String> respCode = soaHeader.getRespCode();

        outputBuf = channelHandlerContext.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        try {
            if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
                writeErrorMessage(channelHandlerContext,
                        context,
                        new SoaException(respCode.get(), soaHeader.getRespMessage().orElse(SoaCode.UnKnown.getMsg())));
            } else {
                messageProcessor.writeHeader(context);
                if (serializer != null && result != null) {
                    messageProcessor.writeBody(serializer, result);
                }
                messageProcessor.writeMessageEnd();
                transport.flush();

                assert (outputBuf.refCnt() == 1);
                channelHandlerContext.writeAndFlush(outputBuf);
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);

            writeErrorMessage(channelHandlerContext,
                    context,
                    new SoaException(SoaCode.UnKnown, e.getMessage()));

            if (outputBuf != null) {
                outputBuf.release();
            }

        }
    }

    private void writeErrorMessage(ChannelHandlerContext ctx,
                                   TransactionContext context,
                                   SoaException e) {

        SoaHeader soaHeader = context.getHeader();

        ByteBuf outputBuf = ctx.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        try {
            soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(e.getMsg()));
            messageProcessor.writeHeader(context);
            messageProcessor.writeMessageEnd();

            transport.flush();

            ctx.writeAndFlush(outputBuf);
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            if (outputBuf != null) {
                outputBuf.release();
            }
        }
    }
}
