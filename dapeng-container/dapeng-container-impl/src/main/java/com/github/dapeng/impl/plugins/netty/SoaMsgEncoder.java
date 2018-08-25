package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.ExceptionUtil;
import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

import static com.github.dapeng.core.helper.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

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
        TransactionContext transactionContext = wrapper.transactionContext;
        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::encode");
        }

        try {
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

                    Long requestTimestamp = (Long) transactionContext.getAttribute("dapeng_request_timestamp");

                    Long cost = System.currentTimeMillis() - requestTimestamp;
                    soaHeader.setCalleeTime2(cost.intValue());
                    soaHeader.setCalleeIp(Optional.of(IPUtils.transferIp(SoaSystemEnvProperties.SOA_CONTAINER_IP)));
                    soaHeader.setCalleePort(Optional.of(SoaSystemEnvProperties.SOA_CONTAINER_PORT));
                    Joiner joiner = Joiner.on(":");
                    soaHeader.setCalleeMid(joiner.join(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName()));
                    soaHeader.setCalleeTid(transactionContext.calleeTid());
                    messageProcessor.writeHeader(transactionContext);
                    if (serializer != null && result != null) {
                        try {
                            messageProcessor.writeBody(serializer, result);
                        } catch (SoaException e) {
                            if (e.getCode().equals(SoaCode.StructFieldNull.getCode())) {
                                e.setCode(SoaCode.ServerRespFieldNull.getCode());
                                e.setMsg(SoaCode.ServerRespFieldNull.getMsg());
                            }
                            throw e;
                        }
                    }
                    messageProcessor.writeMessageEnd();
                    transport.flush();

                    if (LOGGER.isDebugEnabled()) {
                        String debugLog = "response[seqId:" + transactionContext.seqId() + ", respCode:" + respCode.get() + "]:"
                                + "service[" + soaHeader.getServiceName()
                                + "]:version[" + soaHeader.getVersionName()
                                + "]:method[" + soaHeader.getMethodName() + "]"
                                + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : ",")
                                + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : ",")
                                + " calleeTime1:" + soaHeader.getCalleeTime1().orElse(-1) + ","
                                + " calleeTime2:" + soaHeader.getCalleeTime2().orElse(-1);
                        LOGGER.debug(getClass().getSimpleName() + "::encode:" + debugLog + ", payload[seqId:" + transactionContext.seqId() + "]:\n" + result);
                        LOGGER.debug(getClass().getSimpleName() + "::encode, payloadAsByteBuf:\n" + DumpUtil.dumpToStr(out));
                    }
                } catch (Throwable e) {
                    SoaException soaException = ExceptionUtil.convertToSoaException(e);

                    soaHeader.setRespCode(soaException.getCode());
                    soaHeader.setRespMessage(soaException.getMessage());

                    transactionContext.soaException(soaException);
                    writeErrorResponse(transactionContext, application, out);
                }
            }
        } finally {
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
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
                    soaHeader.getRespMessage().orElse(SoaCode.ServerUnKnown.getMsg()));
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

            String infoLog = "response[seqId:" + transactionContext.seqId() + ", respCode:" + soaHeader.getRespCode().get() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "");
            // 根据respCode判断是否是业务异常还是运行时异常
            if (DapengUtil.isDapengCoreException(soaException)) {
                application.error(this.getClass(), infoLog, soaException);
            } else {
                application.info(this.getClass(), infoLog);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(getClass() + " " + infoLog + ", payload:\n" + soaException.getMessage());
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
