package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
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
 * 响应消息编码器
 *
 * @author Ever
 */
@ChannelHandler.Sharable
public class SoaMsgEncoder extends MessageToByteEncoder<SoaResponseWrapper> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaMsgEncoder.class);

    private final Container container;

    SoaMsgEncoder(Container container) {
        this.container = container;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, SoaResponseWrapper wrapper, ByteBuf out) throws Exception {
        ByteBuf outputBuf = null;
        TransactionContext transactionContext = wrapper.transactionContext;
        SoaHeader soaHeader = transactionContext.getHeader();
        Optional<String> respCode = soaHeader.getRespCode();

        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));


        if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
            writeErrorResponse(transactionContext, channelHandlerContext, application);
        } else {
            try {
                BeanSerializer serializer = wrapper.serializer.get();
                Object result = wrapper.result.get();

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

                application.info(this.getClass(),
                        soaHeader.getServiceName()
                                + ":" + soaHeader.getVersionName()
                                + ":" + soaHeader.getMethodName()
                                + " operatorId:" + soaHeader.getOperatorId()
                                + " operatorName:" + soaHeader.getOperatorName()
                                + " response sent");

            } catch (Throwable e) {
                SoaException soaException = ExceptionUtil.convertToSoaException(e);

                soaHeader.setRespCode(Optional.ofNullable(soaException.getCode()));
                soaHeader.setRespMessage(Optional.ofNullable(soaException.getMessage()));

                transactionContext.setSoaException(soaException);
                writeErrorResponse(transactionContext, channelHandlerContext, application);
                if (outputBuf != null) {
                    outputBuf.release();
                }
            }
        }
    }

    private void writeErrorResponse(TransactionContext transactionContext,
                                    ChannelHandlerContext channelHandlerContext,
                                    Application application) {
        SoaHeader soaHeader = transactionContext.getHeader();
        SoaException soaException = transactionContext.getSoaException();
        if (soaException == null) {
            soaException = new SoaException(soaHeader.getRespCode().get(),
                    soaHeader.getRespMessage().orElse(SoaCode.UnKnown.getMsg()));
            transactionContext.setSoaException(soaException);
        }
        ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

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
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            if (outputBuf != null) {
                outputBuf.release();
            }
        }
    }
}
