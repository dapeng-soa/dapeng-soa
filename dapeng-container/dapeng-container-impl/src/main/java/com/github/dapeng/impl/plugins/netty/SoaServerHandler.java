package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.api.*;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.SharedChain;
import com.github.dapeng.core.filter.FilterContextImpl;
import com.github.dapeng.impl.filters.HeadFilter;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.RegistryAgentProxy;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author lihuimin
 * @date 2017/12/7
 */
@ChannelHandler.Sharable
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);

    private final Container container;

    public SoaServerHandler(Container container) {
        this.container = container;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        final long stratTime = System.currentTimeMillis();
        ByteBuf reqMessage = (ByteBuf) msg;
        TSoaTransport inputSoaTransport = new TSoaTransport(reqMessage);
        SoaMessageProcessor parser = new SoaMessageProcessor(inputSoaTransport);

        final TransactionContext context = TransactionContext.Factory.createNewInstance();
        try {
            // parser.service, version, method, header, bodyProtocol
            SoaHeader soaHeader = parser.parseSoaMessage(context);
            context.setHeader(soaHeader);

            SoaServiceDefinition processor = container.getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            container.getDispatcher().execute(() -> {
                try {
                    TransactionContext.Factory.setCurrentInstance(context);
                    processRequest(ctx, parser.getContentProtocol(), processor, reqMessage, context, stratTime);
                } catch (TException e) {
                    LOGGER.error(e.getMessage(), e);
                    writeErrorMessage(ctx, context, new SoaException(SoaCode.UnKnown, e.getMessage() == null ? SoaCode.UnKnown.getMsg() : e.getMessage()));
                } finally {
                    TransactionContext.Factory.removeCurrentInstance();
                }
            });
        } catch (Throwable ex) {
            LOGGER.error(ex.getMessage(), ex);

            // Inside processRequest, reqMessage will be released.
            // But before processRequest invoked, there's still a chance to miss the releasing of reqMessage.
            reqMessage.release();

            if (context.getHeader() == null)
                context.setHeader(new SoaHeader());
            writeErrorMessage(ctx, context, new SoaException(SoaCode.UnKnown, "读请求异常"));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }

    private <I, REQ, RESP> void processRequest(ChannelHandlerContext channelHandlerContext, TProtocol contentProtocol, SoaServiceDefinition<I> serviceDef,
                                               ByteBuf reqMessage, TransactionContext context, long startTime) throws TException {

        try {
            SoaHeader soaHeader = context.getHeader();
            Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            if (application == null){
                throw new SoaException(SoaCode.NotMatchedService);
            }
            SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());

            if (soaFunction == null){
                throw new SoaException(SoaCode.NotMatchedMethod);
            }
            REQ args = soaFunction.reqSerializer.read(contentProtocol);
            contentProtocol.readMessageEnd();

            I iface = serviceDef.iface;
            //log request
            application.info(this.getClass(), "{} {} {} operatorId:{} operatorName:{} request body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.getOperatorId(), soaHeader.getOperatorName(), formatToString(soaFunction.reqSerializer.toString(args)));

            HeadFilter headFilter = new HeadFilter();
            Filter dispatchFilter = new Filter() {

                private FilterChain getPrevChain(FilterContext ctx) {
                    SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                    return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
                }

                @Override
                public void onEntry(FilterContext ctx, FilterChain next) {
                    try {
                        if (serviceDef.isAsync) {
                            SoaFunctionDefinition.Async asyncFunc = (SoaFunctionDefinition.Async) soaFunction;
                            CompletableFuture<RESP> future = (CompletableFuture<RESP>) asyncFunc.apply(iface, args);
                            future.whenComplete((realResult, ex) -> {
                                TransactionContext.Factory.setCurrentInstance(context);
                                processResult(channelHandlerContext, soaFunction, context, realResult, application, ctx);
                                onExit(ctx, getPrevChain(ctx));
                            });
                        } else {
                            SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;
                            RESP result = (RESP) syncFunction.apply(iface, args);
                            processResult(channelHandlerContext, soaFunction, context, result, application, ctx);
                            onExit(ctx, getPrevChain(ctx));
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        writeErrorMessage(channelHandlerContext, context, new SoaException(SoaCode.UnKnown, e.getMessage()));
                    } // todo handle error
                }

                @Override
                public void onExit(FilterContext ctx, FilterChain prev) {
                    try {
                        prev.onExit(ctx);
                    } catch (TException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            };
            SharedChain sharedChain = new SharedChain(headFilter, container.getFilters(), dispatchFilter, 0);

            FilterContextImpl filterContext = new FilterContextImpl();
            filterContext.setAttach(dispatchFilter, "chain", sharedChain);

            final long waitingTime = System.currentTimeMillis() - startTime;
            long timeout = getTimeout(soaHeader);
            if (waitingTime > timeout) {
                throw new SoaException(SoaCode.TimeOut, "服务端请求超时");
            }
            sharedChain.onEntry(filterContext);
        }catch (SoaException e){
            LOGGER.error(e.getMsg());
            writeErrorMessage(channelHandlerContext, context, new SoaException(e.getCode(), e.getMsg()));
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
            writeErrorMessage(channelHandlerContext, context, new SoaException(SoaCode.UnKnown, e.getMessage()));
        }finally {
            reqMessage.release();
        }
    }

    private void processResult(ChannelHandlerContext channelHandlerContext, SoaFunctionDefinition soaFunction, TransactionContext context, Object result, Application application, FilterContext filterContext) {
        SoaHeader soaHeader = context.getHeader();
        soaHeader.setRespCode(Optional.of("0000"));
        soaHeader.setRespMessage(Optional.of("ok"));
        context.setHeader(soaHeader);
        try {
            application.info(this.getClass(), "{} {} {} operatorId:{} operatorName:{} response body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.getOperatorId(), soaHeader.getOperatorName(), formatToString(soaFunction.respSerializer.toString(result)));

            filterContext.setAttribute("channelHandlerContext", channelHandlerContext);
            filterContext.setAttribute("context", context);
            filterContext.setAttribute("respSerializer", soaFunction.respSerializer);
            filterContext.setAttribute("result", result);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            writeErrorMessage(channelHandlerContext, context, filterContext, new SoaException(SoaCode.UnKnown, e.getMessage()));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext context, FilterContext filterContext, SoaException e) {

        SoaHeader soaHeader = context.getHeader();
        LOGGER.info("{} {} {} response header:{} body:{null}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.toString());

        soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
        soaHeader.setRespMessage(Optional.ofNullable(e.getMessage()));
        filterContext.setAttribute("channelHandlerContext", ctx);
        filterContext.setAttribute("context", context);
    }

    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext context, SoaException e) {
        ByteBuf outputBuf = ctx.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor builder = new SoaMessageProcessor(transport);
        SoaHeader soaHeader = context.getHeader();
        try {
            soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(e.getMsg()));
            builder.writeHeader(context);
            builder.writeMessageEnd();

            transport.flush();

            ctx.writeAndFlush(outputBuf);

            LOGGER.info("{} {} {} response header:{} body:{null}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.toString());
        } catch (Throwable e1) {
            LOGGER.error(e1.getMessage(), e1);
            outputBuf.release();
        }

    }

    private String formatToString(String msg) {
        if (msg == null)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }

    private long getTimeout(SoaHeader soaHeader) {
        long timeout = 0L;
        String serviceKey = soaHeader.getServiceName() + "." + soaHeader.getVersionName() + "." + soaHeader.getMethodName() + ".producer";
        Map<ConfigKey, Object> configs = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Server).getConfig(false, serviceKey);
        long envTimeout = SoaSystemEnvProperties.SOA_SERVICE_SERVER_TIMEOUT.longValue();
        if (null != configs) {
            Long timeoutConfig = (Long) configs.get(ConfigKey.ServerTimeout);
            timeout = (timeoutConfig != null) ? timeoutConfig.longValue() : envTimeout;
        }
        if (timeout == 0L) {
            timeout = (envTimeout == 0) ? 2000L : envTimeout;
        }

        if (timeout > SoaSystemEnvProperties.SOA_MAX_TIMEOUT){
            timeout = SoaSystemEnvProperties.SOA_MAX_TIMEOUT;
        }
        return timeout;
    }

}
