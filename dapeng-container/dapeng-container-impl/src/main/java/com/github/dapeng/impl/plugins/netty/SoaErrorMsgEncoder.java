package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 错误返回的消息编码器(有可能是filter产生,或者压根没经过filter(例如请求反序列化的时候出错))
 *
 * @author Ever
 */
@ChannelHandler.Sharable
public class SoaErrorMsgEncoder extends MessageToByteEncoder<TransactionContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaErrorMsgEncoder.class);

    private final Container container;

    SoaErrorMsgEncoder(Container container) {
        this.container = container;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, TransactionContext transactionContext, ByteBuf out) throws Exception {
        ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        SoaHeader soaHeader = transactionContext.getHeader();
        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));
        SoaException soaException = transactionContext.getSoaException();
        try {
            messageProcessor.writeHeader(transactionContext);
            messageProcessor.writeMessageEnd();

            transport.flush();

            channelHandlerContext.writeAndFlush(outputBuf);

            application.error(this.getClass(),
                    soaHeader.getServiceName()
                            + ":" + soaHeader.getVersionName()
                            + ":" + soaHeader.getMethodName()
                            + " operatorId:" + soaHeader.getOperatorId()
                            + " operatorName:" + soaHeader.getOperatorName(),
                    soaException);
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            if (outputBuf != null) {
                outputBuf.release();
            }
        }
    }
}
