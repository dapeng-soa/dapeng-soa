package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.util.ExceptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

/**
 * 经过Filter处理后的消息的编码器
 *
 * @author Ever
 */
@ChannelHandler.Sharable
public class SoaFilterMsgEncoder extends MessageToByteEncoder<FilterContext> {
    private final Container container;

    SoaFilterMsgEncoder(Container container) {
        this.container = container;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, FilterContext ctx, ByteBuf out) throws Exception {
        ByteBuf outputBuf = null;
        TransactionContext transactionContext = (TransactionContext) ctx.getAttribute("context");
        SoaHeader soaHeader = transactionContext.getHeader();
        Optional<String> respCode = soaHeader.getRespCode();

        try {
            if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
                SoaException soaException = transactionContext.getSoaException();
                if (soaException==null) {
                    soaException = new SoaException(respCode.get(),
                            soaHeader.getRespMessage().orElse(SoaCode.UnKnown.getMsg()));
                    transactionContext.setSoaException(soaException);
                }
                channelHandlerContext.writeAndFlush(transactionContext);
            } else {
                BeanSerializer serializer = (BeanSerializer) ctx.getAttribute("respSerializer");
                Object result = ctx.getAttribute("result");

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

                Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

                application.info(this.getClass(),
                        soaHeader.getServiceName()
                                + ":" + soaHeader.getVersionName()
                                + ":" + soaHeader.getMethodName()
                                + " operatorId:" + soaHeader.getOperatorId()
                                + " operatorName:" + soaHeader.getOperatorName()
                                + " response sent");

            }
        } catch (Throwable e) {
            SoaException soaException = ExceptionUtil.convertToSoaException(e);

            soaHeader.setRespCode(Optional.ofNullable(soaException.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(soaException.getMessage()));

            transactionContext.setSoaException(soaException);
            channelHandlerContext.writeAndFlush(transactionContext);

            if (outputBuf != null) {
                outputBuf.release();
            }
        }
    }
}
