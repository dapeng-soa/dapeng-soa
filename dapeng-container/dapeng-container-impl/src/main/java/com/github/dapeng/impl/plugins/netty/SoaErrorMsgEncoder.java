package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.FilterContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

/**
 * 错误返回的消息编码器
 *
 * @author Ever
 */
@ChannelHandler.Sharable
public class SoaErrorMsgEncoder extends MessageToByteEncoder<SoaException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaErrorMsgEncoder.class);

    private final Container container;

    SoaErrorMsgEncoder(Container container) {
        this.container = container;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, SoaException soaException, ByteBuf out) throws Exception {
        TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = transactionContext.getHeader();

        ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        try {
            soaHeader.setRespCode(Optional.ofNullable(soaException.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(soaException.getMsg()));
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
            application.info(this.getClass(),
                    soaHeader.getServiceName()
                            + ":" + soaHeader.getVersionName()
                            + ":" + soaHeader.getMethodName()
                            + " operatorId:" + soaHeader.getOperatorId()
                            + " operatorName:" + soaHeader.getOperatorName()
                            + " exception:" + soaException.getCode() + "-" + soaException.getMsg());
        } catch (Exception e1) {
            LOGGER.error(e1.getMessage(), e1);
            if (outputBuf != null) {
                outputBuf.release();
            }
        }

        LOGGER.error(soaException.getMsg(), soaException);
    }
}
