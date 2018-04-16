package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.*;
import com.github.dapeng.impl.filters.HeadFilter;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocolException;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.RegistryAgentProxy;
import com.github.dapeng.util.DumpUtil;
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

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

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
        final long invokeTime = System.currentTimeMillis();
        ByteBuf reqMessage = (ByteBuf) msg;
        Integer requestFlow = getRequestLength(reqMessage) + Integer.BYTES;
        ByteBuf reqMirror = reqMessage.slice(); // only used for debug packet

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
                    processRequest(ctx, parser.getContentProtocol(), processor, reqMirror, context, invokeTime,requestFlow);
                } catch (Throwable e) {
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

            if (context.getHeader() == null) {
                context.setHeader(new SoaHeader());
            }
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

    private <I, REQ, RESP> void processRequest(ChannelHandlerContext channelHandlerContext,
                                               TProtocol contentProtocol,
                                               SoaServiceDefinition<I> serviceDef,
                                               ByteBuf reqMirror,
                                               TransactionContext context,
                                               long invokeTime,Integer requestFlow) throws TException {

        try {
            SoaHeader soaHeader = context.getHeader();
            //check if request expired
            final long waitingTime = System.currentTimeMillis() - invokeTime;
            long timeout = getTimeout(soaHeader);
            if (waitingTime > timeout) {
                throw new SoaException(SoaCode.TimeOut, "服务端请求超时");
            }

            Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            if (application == null) {
                throw new SoaException(SoaCode.NotMatchedService);
            }
            SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());

            if (soaFunction == null) {
                throw new SoaException(SoaCode.NotMatchedMethod);
            }

            REQ args;
            try {
                args = soaFunction.reqSerializer.read(contentProtocol);
            } catch (TProtocolException | OutOfMemoryError e) {
                //反序列化出错
                LOGGER.error(e.getMessage(), e);
                LOGGER.error(DumpUtil.dumpToStr(reqMirror));
                throw e;
            }
            contentProtocol.readMessageEnd();

            I iface = serviceDef.iface;
            //log request //TODO 需要改成filter
            boolean logFormatEnable = SoaSystemEnvProperties.SOA_LOG_FORMAT_ENABLE;
            application.info(this.getClass(), "{} {} {} operatorId:{} operatorName:{} request body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.getOperatorId(), soaHeader.getOperatorName(), logFormatEnable ? formatToString(soaFunction.reqSerializer.toString(args)) : soaFunction.reqSerializer.toString(args));

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
                                if (ex != null) {
                                    SoaException soaException = convertToSoaException(ex);
                                    writeErrorMessage(channelHandlerContext, context, ctx, soaException);
                                } else {
                                    TransactionContext.Factory.setCurrentInstance(context);
                                    processResult(channelHandlerContext, soaFunction, context, realResult, application, ctx);
                                }
                                onExit(ctx, getPrevChain(ctx));
                            });
                        } else {
                            SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;
                            RESP result = (RESP) syncFunction.apply(iface, args);
                            processResult(channelHandlerContext, soaFunction, context, result, application, ctx);
                            onExit(ctx, getPrevChain(ctx));
                        }
                    } catch (SoaException soaException) {
                        LOGGER.error(soaException.getMessage(), soaException);
                        ctx.setAttribute("isSuccess", false);
                        writeErrorMessage(channelHandlerContext, context, ctx,soaException);
                        onExit(ctx,getPrevChain(ctx));
                    } catch (Throwable e) {
                        LOGGER.error(e.getMessage(), e);
                        ctx.setAttribute("isSuccess", false);
                        String errMsg = e.getCause() != null ? e.getCause().toString() : (e.getMessage() != null ? e.getMessage().toString() : e.toString());
                        writeErrorMessage(channelHandlerContext, context, ctx ,new SoaException(SoaCode.UnKnown,
                                errMsg == null ? SoaCode.UnKnown.getMsg() : errMsg));
                        onExit(ctx,getPrevChain(ctx));
                    }
                }

                @Override
                public void onExit(FilterContext ctx, FilterChain prev) {
                    try {
                        prev.onExit(ctx);
                    } catch (SoaException e) {
                        LOGGER.error(e.getMessage(), e);
                        ctx.setAttribute("isSuccess", false);
                    }
                }
            };
            SharedChain sharedChain = new SharedChain(headFilter, container.getFilters(), dispatchFilter, 0);

            FilterContextImpl filterContext = new FilterContextImpl();
            filterContext.setAttach(dispatchFilter, "chain", sharedChain);
            filterContext.setAttribute("serviceDef",serviceDef);
            filterContext.setAttribute("soaHeader",soaHeader);
            filterContext.setAttribute("waitingTime",waitingTime);
            filterContext.setAttribute("startTime",invokeTime);
            filterContext.setAttribute("requestFlow",requestFlow);
            sharedChain.onEntry(filterContext);
        } catch (SoaException e) {
            LOGGER.error(e.getMsg(), e);
            writeErrorMessage(channelHandlerContext, context, new SoaException(e.getCode(), e.getMsg()));
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            writeErrorMessage(channelHandlerContext, context, new SoaException(SoaCode.UnKnown, e.getMessage()));
        } finally {
            reqMirror.release();
        }
    }

    private void processResult(ChannelHandlerContext channelHandlerContext, SoaFunctionDefinition soaFunction, TransactionContext context, Object result, Application application, FilterContext filterContext) {
        SoaHeader soaHeader = context.getHeader();
        soaHeader.setRespCode(Optional.of(SOA_NORMAL_RESP_CODE));
        soaHeader.setRespMessage(Optional.of("ok"));
        context.setHeader(soaHeader);
        try {
            //TODO 需要改成filter
            boolean logFormatEnable = SoaSystemEnvProperties.SOA_LOG_FORMAT_ENABLE;
            application.info(this.getClass(), "{} {} {} operatorId:{} operatorName:{} response body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.getOperatorId(), soaHeader.getOperatorName(), logFormatEnable ? formatToString(soaFunction.respSerializer.toString(result)) : soaFunction.respSerializer.toString(result));

            filterContext.setAttribute("channelHandlerContext", channelHandlerContext);
            filterContext.setAttribute("context", context);
            filterContext.setAttribute("reqSerializer", soaFunction.reqSerializer);
            filterContext.setAttribute("respSerializer", soaFunction.respSerializer);
            filterContext.setAttribute("result", result);

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            writeErrorMessage(channelHandlerContext, context, filterContext, new SoaException(SoaCode.UnKnown,
                    e.getMessage() == null ? SoaCode.UnKnown.getMsg() : e.getMessage()));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    /**
     * handle this within HeadFilter
     *
     * @param ctx
     * @param context
     * @param filterContext
     * @param e
     */
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
        if (msg == null) {
            return msg;
        }

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len) {
            msg = msg.substring(0, 128) + "...(" + len + ")";
        }

        return msg;
    }

    /**
     * TODO
     * 获取timeout参数. 优先级如下:
     * 1. 如果invocationContext有设置的话, 那么用invocationContext的(这个值每次调用都可能不一样)
     * 2. invocationContext没有的话, 就拿Option的(命令行或者环境变量)
     * 3. 没设置Option的话, 那么取ZK的.
     * 4. ZK没有的话, 拿IDL的(暂没实现该参数)
     * 5. 都没有的话, 拿默认值.(这个值所有方法一致, 假设为50S)
     * <p>
     * 最后校验一下,拿到的值不能超过系统设置的最大值
     * <p>
     * 如果得到的数据超过最大值, 那么就用最大值.
     *
     * @param soaHeader
     * @return
     */
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
            timeout = (envTimeout == 0) ? SoaSystemEnvProperties.SOA_DEFAULT_TIMEOUT : envTimeout;
        }

        if (timeout > SoaSystemEnvProperties.SOA_MAX_TIMEOUT) {
            timeout = SoaSystemEnvProperties.SOA_MAX_TIMEOUT;
        }
        return timeout;
    }

    private SoaException convertToSoaException(Throwable ex) {
        SoaException soaException = null;
        if (ex instanceof SoaException) {
            soaException = (SoaException) ex;
        } else {
            soaException = new SoaException(SoaCode.UnKnown.getCode(), ex.getMessage());
        }
        return soaException;
    }



    private int getRequestLength(ByteBuf inputBuf) {
        int readerIndex = inputBuf.readerIndex();
        int requestLength = inputBuf.readInt();
        inputBuf.readerIndex(readerIndex);
        return requestLength;
    }

}
