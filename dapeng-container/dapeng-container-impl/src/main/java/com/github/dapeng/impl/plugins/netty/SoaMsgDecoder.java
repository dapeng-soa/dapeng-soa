package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocolException;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.dapeng.util.ExceptionUtil.convertToSoaException;

/**
 * @author ever
 */
@ChannelHandler.Sharable
public class SoaMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaMsgDecoder.class);

    private final Container container;

    SoaMsgDecoder(Container container) {
        this.container = container;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List out) throws Exception {
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(getClass().getSimpleName() + "::decode");
            }

            Object request = parseSoaMsg(msg);

            final TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();
            /**
             * use AttributeMap to share common data on different  ChannelHandlers
             */
            Attribute<Map<Integer, Long>> requestTimestampAttr = ctx.channel().attr(NettyChannelKeys.REQUEST_TIMESTAMP);

            Map<Integer, Long> requestTimestampMap = requestTimestampAttr.get();
            if (requestTimestampMap == null) {
                requestTimestampMap = new HashMap<>(64);
            }
            requestTimestampMap.put(transactionContext.getSeqid(), System.currentTimeMillis());

            requestTimestampAttr.set(requestTimestampMap);

            out.add(request);
        } catch (Throwable e) {

            SoaException soaException = convertToSoaException(e);
            TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();

            SoaHeader soaHeader = transactionContext.getHeader();
            soaHeader.setRespCode(Optional.ofNullable(soaException.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(soaException.getMessage()));

            transactionContext.setSoaException(soaException);
            SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                    Optional.ofNullable(null),
                    Optional.ofNullable(null));

            TransactionContext.Factory.removeCurrentInstance();

            ctx.writeAndFlush(responseWrapper);
        }
    }

    private <I, REQ, RESP> REQ parseSoaMsg(ByteBuf msg) throws TException {
        TSoaTransport inputSoaTransport = new TSoaTransport(msg);
        SoaMessageProcessor parser = new SoaMessageProcessor(inputSoaTransport);

        final TransactionContext context = TransactionContext.Factory.createNewInstance();

        // parser.service, version, method, header, bodyProtocol
        SoaHeader soaHeader = parser.parseSoaMessage(context);
        context.setHeader(soaHeader);

        updateTransactionCtx(context,soaHeader);

        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        if (application == null) {
            throw new SoaException(SoaCode.NotMatchedService);
        }

        SoaServiceDefinition processor = container.getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) processor.functions.get(soaHeader.getMethodName());

        if (soaFunction == null) {
            throw new SoaException(SoaCode.NotMatchedMethod);
        }

        TProtocol contentProtocol = parser.getContentProtocol();
        REQ args;
        try {
            args = soaFunction.reqSerializer.read(contentProtocol);
        } catch (TProtocolException | OutOfMemoryError e) {
            //反序列化出错
            LOGGER.error(DumpUtil.dumpToStr(msg));
            throw e;
        }
        contentProtocol.readMessageEnd();

        boolean logFormatEnable = SoaSystemEnvProperties.SOA_LOG_FORMAT_ENABLE;
        String infoLog = "request[seqId:" + context.getSeqid() + "]:"
                + "service[" + soaHeader.getServiceName()
                + "]:version[" + soaHeader.getVersionName()
                + "]:method[" + soaHeader.getMethodName() + "]"
                + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                + (soaHeader.getOperatorName().isPresent() ? " operatorName:" + soaHeader.getOperatorName().get() : "")
                + " args:[" + (logFormatEnable ? formatToString(soaFunction.reqSerializer.toString(args)) : soaFunction.reqSerializer.toString(args)) + "]";

        application.info(this.getClass(), infoLog);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getClass().getSimpleName() + "::decode " + infoLog + ", payload:\n" + soaFunction.reqSerializer.toString(args));
        }
        return args;
    }

    private void updateTransactionCtx(TransactionContext ctx, SoaHeader soaHeader)  {
        ctx.setCallerFrom(soaHeader.getCallerFrom());
        ctx.setCallerIp(soaHeader.getCallerIp());
        ctx.setCustomerId(soaHeader.getCustomerId());
        ctx.setCustomerName(soaHeader.getCustomerName());
        ctx.setOperatorId(soaHeader.getOperatorId());
        ctx.setOperatorName(soaHeader.getOperatorName());
    }

    private static String formatToString(String msg) {
        if (msg == null)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }

}
