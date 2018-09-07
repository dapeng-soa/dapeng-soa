package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.api.Container;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.*;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.impl.filters.HeadFilter;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.zookeeper.ServerZkAgentImpl;
import com.github.dapeng.registry.zookeeper.ZkServiceInfo;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.ExceptionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.dapeng.core.helper.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * @author lihuimin
 * @date 2017/12/7
 */
@ChannelHandler.Sharable
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);

    private final Container container;

    SoaServerHandler(Container container) {
        this.container = container;
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) {
        final long invokeTime = System.currentTimeMillis();
        final TransactionContext transactionContext = TransactionContext.Factory.currentInstance();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::read");
        }

        try {
            SoaHeader soaHeader = transactionContext.getHeader();

            SoaServiceDefinition processor = container.getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            Executor dispatcher = container.getDispatcher();

            if (LOGGER.isDebugEnabled() && SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL) {
                ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) dispatcher;
                LOGGER.debug("BizThreadPoolInfo:\n"
                        + DumpUtil.dumpThreadPool(poolExecutor));
            }
            dispatcher.execute(() -> {
                try {
                    TransactionContext.Factory.currentInstance(transactionContext);
                    processRequest(channelHandlerContext, processor, msg, transactionContext, invokeTime);
                } catch (Throwable e) {
                    writeErrorMessage(channelHandlerContext,
                            transactionContext,
                            ExceptionUtil.convertToSoaException(e));
                } finally {
                    TransactionContext.Factory.removeCurrentInstance();
                }
            });
        } catch (Throwable ex) {
            if (transactionContext.getHeader() == null) {
                LOGGER.error("should not come here. soaHeader is null");
                ((TransactionContextImpl) transactionContext).setHeader(new SoaHeader());
            }
            writeErrorMessage(channelHandlerContext, transactionContext, new SoaException(SoaCode.ServerUnKnown.getCode(), "读请求异常", ex));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Uncaught exceptions from inbound handlers will propagate up to this handler
        TransactionContext tranCtx = TransactionContextImpl.Factory.currentInstance();
        // short error log and detail error log, for the sake of elasticsearch indexing
        LOGGER.error("exceptionCaught:seqId:" + (tranCtx == null ? "" : tranCtx.seqId()) + ", channel:" + ctx.channel() + ", msg:" + cause.getMessage());
        LOGGER.error("exceptionCaught:seqId:" + (tranCtx == null ? "" : tranCtx.seqId()) + ", " + cause.getMessage(), cause);
        ctx.close();
    }

    private <I, REQ, RESP> void processRequest(ChannelHandlerContext channelHandlerContext,
                                               SoaServiceDefinition<I> serviceDef,
                                               REQ args,
                                               TransactionContext transactionContext,
                                               long invokeTime) throws TException {
        try {
            SoaHeader soaHeader = transactionContext.getHeader();

            //check if request expired
            final long waitingTime = System.currentTimeMillis() - invokeTime;
            //fixme if zk down ?
            long timeout = soaHeader.getTimeout().map(Long::valueOf).orElse(getTimeout(soaHeader));
            if (waitingTime > timeout) {
                if (LOGGER.isDebugEnabled()) {
                    int seqId = transactionContext.seqId();
                    String debugLog = "request[seqId=" + seqId + ", waitingTime=" + waitingTime + "] timeout:"
                            + "service[" + soaHeader.getServiceName()
                            + "]:version[" + soaHeader.getVersionName()
                            + "]:method[" + soaHeader.getMethodName() + "]"
                            + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                            + (soaHeader.getUserId().isPresent() ? " userId:" + soaHeader.getUserId().get() : "");

                    LOGGER.debug(getClass().getSimpleName() + "::processRequest " + debugLog);
                }
                throw new SoaException(SoaCode.ServerReqTimeOut, "服务端请求超时");
            }

            Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            if (application == null) {
                throw new SoaException(SoaCode.NoMatchedService);
            }
            //设置服务方法最大执行时间(慢服务)
            transactionContext.maxProcessTime(application.getMethodMaxProcessTime(soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName()));

            SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());

            if (soaFunction == null) {
                throw new SoaException(SoaCode.ServerNoMatchedMethod);
            }

            HeadFilter headFilter = new HeadFilter();
            Filter dispatchFilter = new DispatchFilter(serviceDef, soaFunction, args);

            SharedChain sharedChain = new SharedChain(headFilter, container.getFilters(), dispatchFilter, 0);

            FilterContextImpl filterContext = new FilterContextImpl();
            filterContext.setAttribute("channelHandlerContext", channelHandlerContext);
            filterContext.setAttribute("context", transactionContext);
            filterContext.setAttribute("application", application);
            filterContext.setAttribute("isAsync", serviceDef.isAsync);
            filterContext.setAttach(dispatchFilter, "chain", sharedChain);

            sharedChain.onEntry(filterContext);
        } catch (SoaException e) {
            // can't reach the headFilter
            writeErrorMessage(channelHandlerContext, transactionContext, e);
        } catch (Throwable e) {
            // can't reach the headFilter
            writeErrorMessage(channelHandlerContext, transactionContext, ExceptionUtil.convertToSoaException(e));
        }
    }


    /**
     * we can't handle this within HeadFilter as sometimes we can't reach the headFilter(errors that outside the filterChain)
     * so we write the transactionContext to outbound
     *
     * @param ctx
     * @param transactionContext
     * @param e
     */
    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext transactionContext, SoaException e) {
        LOGGER.error("writeErrorMessage: " + ctx.channel(), e);

        attachErrorInfo(transactionContext, e);

        SoaResponseWrapper responseWrapper = new SoaResponseWrapper(transactionContext,
                Optional.ofNullable(null),
                Optional.ofNullable(null));

        ctx.writeAndFlush(responseWrapper).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }


    /**
     * attach errorInfo to transactionContext, so that we could handle it with HeadFilter
     *
     * @param transactionContext
     * @param e
     */
    private void attachErrorInfo(TransactionContext transactionContext, SoaException e) {
        SoaHeader soaHeader = transactionContext.getHeader();
        soaHeader.setRespCode(e.getCode());
        soaHeader.setRespMessage(e.getMsg());
        transactionContext.soaException(e);
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
        String serviceKey = soaHeader.getServiceName();
        ZkServiceInfo configInfo = ServerZkAgentImpl.getInstance().getConfig(false, serviceKey);

        long envTimeout = SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT;
        if (null != configInfo) {
            //方法级别
            Long methodTimeOut = configInfo.timeConfig.serviceConfigs.get(soaHeader.getMethodName());
            //服务配置
            Long serviceTimeOut = configInfo.timeConfig.serviceConfigs.get(ConfigKey.TimeOut.getValue());
            //全局
            Long globalTimeOut = configInfo.timeConfig.globalConfig;

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(getClass().getSimpleName() + "::timeout request:serviceName:{},methodName:{}," +
                                " methodTimeOut:{},serviceTimeOut:{},globalTimeOut:{}",
                        soaHeader.getServiceName(), soaHeader.getMethodName(), methodTimeOut, serviceTimeOut, globalTimeOut);
            }

            Long timeoutConfig;

            if (methodTimeOut != null) {
                timeoutConfig = methodTimeOut;
            } else if (serviceTimeOut != null) {
                timeoutConfig = serviceTimeOut;
            } else if (globalTimeOut != null) {
                timeoutConfig = globalTimeOut;
            } else {
                timeoutConfig = null;
            }

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


    class DispatchFilter<I, REQ, RESP> implements Filter {
        private final SoaServiceDefinition<I> serviceDef;
        private final REQ args;
        private final SoaFunctionDefinition<I, REQ, RESP> soaFunction;

        DispatchFilter(SoaServiceDefinition<I> serviceDef,
                       SoaFunctionDefinition<I, REQ, RESP> soaFunction,
                       REQ args) {

            this.serviceDef = serviceDef;
            this.args = args;
            this.soaFunction = soaFunction;
        }

        private FilterChain getPrevChain(FilterContext ctx) {
            SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
            return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
        }

        @Override
        public void onEntry(FilterContext filterContext, FilterChain next) {

            I iface = serviceDef.iface;
            TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");

            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(SoaServerHandler.class.getSimpleName() + "$dispatchFilter::onEntry[seqId:"
                            + transactionContext.seqId() + ", async:" + serviceDef.isAsync + "]");
                }
                if (serviceDef.isAsync) {
                    SoaFunctionDefinition.Async asyncFunc = (SoaFunctionDefinition.Async) soaFunction;

                    CompletableFuture<RESP> future = (CompletableFuture<RESP>) asyncFunc.apply(iface, args);

                    future.whenComplete((realResult, ex) -> {
                        try {
                            TransactionContext.Factory.currentInstance(transactionContext);

                            if (ex != null) {
                                SoaException soaException = ExceptionUtil.convertToSoaException(ex);
                                attachErrorInfo(transactionContext, soaException);
                            } else {
                                processResult(soaFunction, transactionContext, realResult, filterContext);
                            }
                            onExit(filterContext, getPrevChain(filterContext));
                        } finally {
                            TransactionContext.Factory.removeCurrentInstance();
                        }
                    });
                } else {
                    SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;

                    RESP result = (RESP) syncFunction.apply(iface, args);

                    processResult(soaFunction, transactionContext, result, filterContext);
                    onExit(filterContext, getPrevChain(filterContext));
                }
            } catch (Throwable e) {
                attachErrorInfo(transactionContext, ExceptionUtil.convertToSoaException(e));
                onExit(filterContext, getPrevChain(filterContext));
            }
        }

        @Override
        public void onExit(FilterContext filterContext, FilterChain prev) {
            TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");

            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(SoaServerHandler.class.getSimpleName() + "$dispatchFilter::onExit[seqId:"
                            + transactionContext.seqId() + "]");
                }
                prev.onExit(filterContext);
            } catch (TException e) {
                attachErrorInfo(transactionContext, ExceptionUtil.convertToSoaException(e));
            }
        }

        private void processResult(SoaFunctionDefinition soaFunction,
                                   TransactionContext transactionContext,
                                   Object result,
                                   FilterContext filterContext) {
            SoaHeader soaHeader = transactionContext.getHeader();
            soaHeader.setRespCode(SOA_NORMAL_RESP_CODE);
            soaHeader.setRespMessage("ok");
            try {
                filterContext.setAttribute("reqSerializer", soaFunction.reqSerializer);
                filterContext.setAttribute("respSerializer", soaFunction.respSerializer);
                filterContext.setAttribute("result", result);

            } catch (Throwable e) {
                attachErrorInfo(transactionContext, ExceptionUtil.convertToSoaException(e));
            }
        }
    }
}
