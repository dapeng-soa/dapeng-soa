package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.FilterContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

/**
 * 正常返回的消息编码器
 *
 * @author Ever
 */
public class SoaNormalMsgEncoder extends MessageToByteEncoder<FilterContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaNormalMsgEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, FilterContext ctx, ByteBuf out) throws Exception {
        ByteBuf outputBuf = null;
        TransactionContext transactionContext = (TransactionContext) ctx.getAttribute("context");
        BeanSerializer serializer = (BeanSerializer) ctx.getAttribute("respSerializer");
        Object result = ctx.getAttribute("result");
        SoaHeader soaHeader = transactionContext.getHeader();
        Optional<String> respCode = soaHeader.getRespCode();

        try {
            if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
                channelHandlerContext.writeAndFlush(
                        new SoaException(respCode.get(),
                                soaHeader.getRespMessage().orElse(SoaCode.UnKnown.getMsg())));
            } else {
                outputBuf = channelHandlerContext.alloc().buffer(8192);
                TSoaTransport transport = new TSoaTransport(outputBuf);
                SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

                messageProcessor.writeHeader(transactionContext);
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

            channelHandlerContext.writeAndFlush(new SoaException(SoaCode.UnKnown, e.getMessage()));

            if (outputBuf != null) {
                outputBuf.release();
            }
        }
    }
}
