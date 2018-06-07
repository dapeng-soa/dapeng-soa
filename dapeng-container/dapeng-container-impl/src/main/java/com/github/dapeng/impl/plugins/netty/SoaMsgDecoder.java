package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocolException;
import com.github.dapeng.util.DumpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.dapeng.util.ExceptionUtil.convertToSoaException;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

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

            final TransactionContext transactionContext = TransactionContext.Factory.currentInstance();
            /**
             * use AttributeMap to share common data on different  ChannelHandlers
             */
            Attribute<Map<Integer, Long>> requestTimestampAttr = ctx.channel().attr(NettyChannelKeys.REQUEST_TIMESTAMP);

            Map<Integer, Long> requestTimestampMap = requestTimestampAttr.get();
            if (requestTimestampMap == null) {
                requestTimestampMap = new HashMap<>(64);
            }
            requestTimestampMap.put(transactionContext.seqId(), System.currentTimeMillis());

            requestTimestampAttr.set(requestTimestampMap);

            out.add(request);
        } catch (Throwable e) {

            SoaException soaException = convertToSoaException(e);
            TransactionContext transactionContext = TransactionContext.Factory.currentInstance();

            SoaHeader soaHeader = transactionContext.getHeader();
            soaHeader.setRespCode(soaException.getCode());
            soaHeader.setRespMessage(soaException.getMessage());

            transactionContext.soaException(soaException);
            SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                    Optional.ofNullable(null),
                    Optional.ofNullable(null));

            TransactionContext.Factory.removeCurrentInstance();

            ctx.writeAndFlush(responseWrapper).addListener(FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    private <I, REQ, RESP> REQ parseSoaMsg(ByteBuf msg) throws TException {
        TSoaTransport inputSoaTransport = new TSoaTransport(msg);
        SoaMessageProcessor parser = new SoaMessageProcessor(inputSoaTransport);

        final TransactionContext context = TransactionContext.Factory.createNewInstance();

        // parser.service, version, method, header, bodyProtocol
        SoaHeader soaHeader = parser.parseSoaMessage(context);
        ((TransactionContextImpl)context).setHeader(soaHeader);

        updateTransactionCtx((TransactionContextImpl)context, soaHeader);

        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, context.sessionTid().orElse("0"));

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

        if (LOGGER.isDebugEnabled()) {
            String debugLog = "request[seqId:" + context.seqId() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "") + " "
                    + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "") + " "
                    + (soaHeader.getUserIp().isPresent() ? " userIp:" + soaHeader.getUserIp().get() : "");
            LOGGER.debug(getClass().getSimpleName() + "::decode " + debugLog + ", payload:\n" + args);
        }
        return args;
    }

    private void updateTransactionCtx(TransactionContextImpl ctx, SoaHeader soaHeader) {
        if (soaHeader.getCallerMid().isPresent()) {
            ctx.callerMid(soaHeader.getCallerMid().get());
        }
        ctx.callerIp(soaHeader.getCallerIp().orElse(null));
        if (soaHeader.getUserId().isPresent()) {
            ctx.userId(soaHeader.getUserId().get());
        }
        if (soaHeader.getCallerPort().isPresent()) {
            ctx.callerPort(soaHeader.getCallerPort().get());
        }
        if (soaHeader.getOperatorId().isPresent()) {
            ctx.operatorId(soaHeader.getOperatorId().get());
        }
        if (soaHeader.getCallerTid().isPresent()) {
            ctx.callerTid(soaHeader.getCallerTid().get());
        }

        ctx.calleeTid(DapengUtil.generateTid());
        ctx.sessionTid(soaHeader.getSessionTid().orElse(ctx.calleeTid()));
    }
}
