package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.ExceptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          SoaResponseWrapper wrapper,
                          ByteBuf out) throws Exception {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::encode");
        }


        TransactionContext transactionContext = wrapper.transactionContext;
        SoaHeader soaHeader = transactionContext.getHeader();
        Optional<String> respCode = soaHeader.getRespCode();

        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));


        if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
            writeErrorResponse(transactionContext, application, out);
        } else {
            try {
                BeanSerializer serializer = wrapper.serializer.get();
                Object result = wrapper.result.get();

                TSoaTransport transport = new TSoaTransport(out);
                SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

                messageProcessor.writeHeader(transactionContext);
                if (serializer != null && result != null) {
                    messageProcessor.writeBody(serializer, result);
                }
                messageProcessor.writeMessageEnd();
                transport.flush();

                /**
                 * use AttributeMap to share common data on different  ChannelHandlers
                 */
                Attribute<Map<Integer, Long>> requestTimestampAttr = channelHandlerContext.channel().attr(NettyChannelKeys.REQUEST_TIMESTAMP);
                Map<Integer, Long> requestTimestampMap = requestTimestampAttr.get();

                Long requestTimestamp = 0L;
                if (requestTimestampMap != null) {
                    requestTimestamp = requestTimestampMap.getOrDefault(transactionContext.getSeqid(), requestTimestamp);
                    //each per request take the time then remove it
                    requestTimestampMap.remove(transactionContext.getSeqid());
                } else {
                    LOGGER.warn(getClass().getSimpleName() + "::encode no requestTimestampMap found!");
                }

                String infoLog = "response[seqId:" + transactionContext.getSeqid() + ", respCode:" + respCode.get() + "]:"
                        + "service[" + soaHeader.getServiceName()
                        + "]:version[" + soaHeader.getVersionName()
                        + "]:method[" + soaHeader.getMethodName() + "]"
                        + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                        + (soaHeader.getOperatorId().isPresent() ? " operatorName:" + soaHeader.getOperatorName().get() : ""
                        + " cost:" + (System.currentTimeMillis() - requestTimestamp) + "ms");

                application.info(this.getClass(), infoLog);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(getClass().getSimpleName() + "::encode " + infoLog + ", payload:\n" + result);
                    LOGGER.debug(getClass().getSimpleName() + "::encode " + DumpUtil.dumpToStr(out));
                }
            } catch (Throwable e) {
                SoaException soaException = ExceptionUtil.convertToSoaException(e);

                soaHeader.setRespCode(Optional.ofNullable(soaException.getCode()));
                soaHeader.setRespMessage(Optional.ofNullable(soaException.getMessage()));

                transactionContext.soaException(soaException);
                writeErrorResponse(transactionContext, application, out);
            }
        }
    }

    /**
     * override the initialCapacity to 1024
     *
     * @param ctx
     * @param msg
     * @param preferDirect
     * @return
     * @throws Exception
     */
    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, @SuppressWarnings("unused") SoaResponseWrapper msg,
                                     boolean preferDirect) throws Exception {
        if (preferDirect) {
            return ctx.alloc().ioBuffer(5120);
        } else {
            return ctx.alloc().heapBuffer(5120);
        }
    }

    private void writeErrorResponse(TransactionContext transactionContext,
                                    Application application,
                                    ByteBuf out) {
        SoaHeader soaHeader = transactionContext.getHeader();
        SoaException soaException = transactionContext.soaException();
        if (soaException == null) {
            soaException = new SoaException(soaHeader.getRespCode().get(),
                    soaHeader.getRespMessage().orElse(SoaCode.UnKnown.getMsg()));
            transactionContext.soaException(soaException);
        }

        //Reuse the byteBuf
        if (out.readableBytes() > 0) {
            out.clear();
        }

        TSoaTransport transport = new TSoaTransport(out);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        try {
            messageProcessor.writeHeader(transactionContext);
            messageProcessor.writeMessageEnd();

            transport.flush();

            String infoLog = "response[seqId:" + transactionContext.getSeqid() + ", respCode:" + soaHeader.getRespCode().get() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                    + (soaHeader.getOperatorId().isPresent() ? " operatorName:" + soaHeader.getOperatorName().get() : "");
            application.error(this.getClass(), infoLog, soaException);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(getClass() + " " + infoLog + ", payload:\n" + soaException.getMessage());
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
