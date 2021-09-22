/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.client.netty;

import com.github.dapeng.client.filter.LogFilter;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.*;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.SoaMessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dapeng.core.helper.IPUtils.transferIp;

/**
 * @author lihuimin
 */
@SuppressWarnings("unchecked")
public abstract class SoaBaseConnection implements SoaConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaBaseConnection.class);

    private final String host;
    private final int port;
    private final static SoaConnectionPoolFactory factory = ServiceLoader.load(SoaConnectionPoolFactory.class,
            SoaBaseConnection.class.getClassLoader()).iterator().next();
    private Channel channel = null;
    private NettyClient client;
    private final static AtomicInteger seqidAtomic = new AtomicInteger(0);
    private ClientRefManager clientRefManager = ClientRefManager.getInstance();


    SoaBaseConnection(String host, int port) {
        this.client = NettyClientFactory.getNettyClient();
        this.host = host;
        this.port = port;
        try {
            channel = connect(host, port);
        } catch (Exception e) {
            LOGGER.error("connect to {}:{} failed", host, port);
        }
    }


    @Override
    public <REQ, RESP> RESP send(
            String service, String version,
            String method, REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer,
            long timeout)
            throws SoaException {
        int seqid = seqidAtomic.getAndIncrement();

        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        invocationContext.seqId(seqid);

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }

            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("dispatchFilter::onEntry");
                }
                ByteBuf requestBuf = buildRequestBuf(service, version, method, seqid, request, requestSerializer);

                // TODO filter
                checkChannel();

                try {
                    ByteBuf responseBuf = client.send(channel, seqid, requestBuf, timeout, service);

                    Result<RESP> result = processResponse(responseBuf, responseSerializer);
                    ctx.setAttribute("result", result);

                    onExit(ctx, getPrevChain(ctx));
                } finally {
                    InvocationContextImpl.Factory.removeCurrentInstance();
                }
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
                prev.onExit(ctx);
            }
        };

        //an empty filter
        Filter headerFilter = new Filter() {
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("headerFilter::onEntry");
                }
                next.onEntry(ctx);
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
                // do nothing
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("headerFilter::onExit");
                }
            }
        };

        List<Filter> shareFilters = new ArrayList<>();
        shareFilters.add(new LogFilter());
        SharedChain sharedChain = new SharedChain(headerFilter, shareFilters, dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);
        filterContext.setAttribute("context", invocationContext);
        filterContext.setAttribute("serverInfo", host + ":" + port);

        sharedChain.onEntry(filterContext);

        Result<RESP> result = (Result<RESP>) filterContext.getAttribute("result");
        assert (result != null);

        //请求响应，在途请求-1
        RuntimeInstance runtimeInstance = clientRefManager.serviceInfo(service).runtimeInstance(host, port);
        if (runtimeInstance == null) {
            LOGGER.error("SoaBaseConnection::runtimeInstance not found.");
        } else {
            runtimeInstance.decreaseActiveCount();
        }

        if (result.success != null) {
            return result.success;
        } else {
            throw result.exception;
        }
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(
            String service, String version,
            String method, REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer,
            long timeout) throws SoaException {

        int seqid = seqidAtomic.getAndIncrement();

        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        invocationContext.seqId(seqid);

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }

            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
                try {

                    ByteBuf requestBuf = buildRequestBuf(service, version, method, seqid, request, requestSerializer);

                    CompletableFuture<ByteBuf> responseBufFuture;
                    try {
                        checkChannel();
                        responseBufFuture = client.sendAsync(channel, seqid, requestBuf, timeout);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        Result<RESP> result = new Result<>(null,
                                new SoaException(SoaCode.ClientUnKnown, SoaCode.ClientUnKnown.getMsg()));
                        ctx.setAttribute("result", result);
                        onExit(ctx, getPrevChain(ctx));
                        return;
                    }

                    responseBufFuture.whenComplete((realResult, ex) -> {
                        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, invocationContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));
                        if (ex != null) {
                            SoaException soaException = convertToSoaException(ex);
                            Result<RESP> result = new Result<>(null, soaException);
                            ctx.setAttribute("result", result);
                        } else {
                            //fixme do it in filter
                            InvocationContextImpl.Factory.currentInstance(invocationContext);

                            Result<RESP> result = processResponse(realResult, responseSerializer);
                            ctx.setAttribute("result", result);
                        }

                        try {
                            onExit(ctx, getPrevChain(ctx));
                        } catch (SoaException e) {
                            LOGGER.error(e.getMessage(), e);
                        } finally {
                            InvocationContextImpl.Factory.removeCurrentInstance();
                            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    SoaException soaException = convertToSoaException(e);
                    Result<RESP> result = new Result<>(null, soaException);

                    if (invocationContext.lastInvocationInfo().responseCode() == null) {
                        ((InvocationInfoImpl) invocationContext.lastInvocationInfo()).responseCode(soaException.getCode());
                    }

                    ctx.setAttribute("result", result);

                    // fix  sendAsync  json序列化异常  LogFilter respCode == null
                    InvocationContextImpl invocationContext = (InvocationContextImpl) ctx.getAttribute("context");
                    InvocationInfoImpl lastInfo = (InvocationInfoImpl) invocationContext.lastInvocationInfo();
                    lastInfo.responseCode(soaException.getCode());
                    lastInfo.calleeIp(transferIp(host));
                    lastInfo.calleePort(port);

                    invocationContext.lastInvocationInfo(lastInfo);
                    ctx.setAttribute("context", invocationContext);

                    onExit(ctx, getPrevChain(ctx));
                } finally {
                    InvocationContextImpl.Factory.removeCurrentInstance();
                    MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
                }
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
                prev.onExit(ctx);
            }
        };

        Filter headFilter = new Filter() {
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
                CompletableFuture<RESP> resultFuture = new CompletableFuture<>();
                ctx.setAttach(this, "future", resultFuture);
                next.onEntry(ctx);
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

                CompletableFuture<RESP> future = (CompletableFuture<RESP>) ctx.getAttach(this, "future");
                Result<RESP> result = (Result<RESP>) ctx.getAttribute("result");
                if (result.success != null) {
                    future.complete(result.success);
                } else {
                    future.completeExceptionally(result.exception);
                }
            }
        };
        // Head, LoadBalance, .. dispatch
        List<Filter> shareFilters = new ArrayList<>();
        shareFilters.add(new LogFilter());

        SharedChain sharedChain = new SharedChain(headFilter, shareFilters, dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);
        filterContext.setAttribute("context", invocationContext);
        filterContext.setAttribute("serverInfo", host + ":" + port);

        try {
            sharedChain.onEntry(filterContext);
        } catch (TException e) {
            throw new SoaException(e);
        }
        CompletableFuture<RESP> resultFuture = (CompletableFuture<RESP>) filterContext.getAttach(headFilter, "future");


        assert (resultFuture != null);
        //请求响应，在途请求-1
        RuntimeInstance runtimeInstance = clientRefManager.serviceInfo(service).runtimeInstance(host, port);
        if (runtimeInstance == null) {
            LOGGER.error("SoaBaseConnection::runtimeInstance not found.");
        } else {
            runtimeInstance.decreaseActiveCount();
        }

        return resultFuture;
    }


    private SoaException convertToSoaException(Throwable ex) {
        SoaException soaException;
        if (ex instanceof SoaException) {
            soaException = (SoaException) ex;
        } else {
            soaException = new SoaException(SoaCode.ClientUnKnown.getCode(), ex.getMessage());
        }
        return soaException;
    }

    protected abstract <REQ> ByteBuf buildRequestBuf(String service, String version, String method, int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException;

    /**
     * 请求的响应. 要不是成功的响应, 要不是异常对象
     *
     * @param <RESP>
     */
    public static class Result<RESP> {
        public final RESP success;
        public final SoaException exception;

        Result(RESP success, SoaException exception) {
            this.success = success;
            this.exception = exception;
        }
    }

    private <RESP> Result<RESP> processResponse(ByteBuf responseBuf, BeanSerializer<RESP> responseSerializer) {
        if (responseBuf == null) {
            return new Result<>(null, new SoaException(SoaCode.ReqTimeOut));
        }
        final int readerIndex = responseBuf.readerIndex();
        try {
            SoaMessageParser parser = new SoaMessageParser(responseBuf, responseSerializer).parseHeader();
            SoaHeader respHeader = parser.getHeader();
            InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
            InvocationInfoImpl lastInfo = (InvocationInfoImpl) invocationContext.lastInvocationInfo();
            fillLastInvocationInfo(lastInfo, respHeader);
            if (SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE.equals(lastInfo.responseCode())) {
                parser.parseBody();
                RESP resp = (RESP) parser.getBody();
                assert (resp != null);
                return new Result<>(resp, null);
            } else {
                return new Result<>(null, new SoaException(
                        lastInfo.responseCode(),
                        (respHeader.getRespMessage().isPresent()) ? respHeader.getRespMessage().get() : SoaCode.ClientUnKnown.getMsg()));
            }

        } catch (SoaException ex) {
            return new Result<>(null, ex);
        } catch (TException | RuntimeException ex) {
            LOGGER.error("通讯包解析出错:\n" + ex.getMessage(), ex);
            LOGGER.error(DumpUtil.dumpToStr(responseBuf.readerIndex(readerIndex)));
            return new Result<>(null,
                    new SoaException(SoaCode.RespDecodeError, SoaCode.RespDecodeError.getMsg()));

        } catch (Throwable ex) {
            LOGGER.error("processResponse unknown exception: " + ex.getMessage(), ex);
            return new Result<>(null,
                    new SoaException(SoaCode.RespDecodeUnknownError, SoaCode.RespDecodeUnknownError.getMsg()));
        } finally {
            responseBuf.release();
        }
    }


    /**
     * 创建连接
     */
    private synchronized Channel connect(String host, int port) throws SoaException {
        if (channel != null && channel.isActive()) {
            return channel;
        }

        try {
            return channel = this.client.connect(host, port);
        } catch (Exception e) {
            throw new SoaException(SoaCode.NotConnected);
        }
    }

    private void checkChannel() throws SoaException {
        if (channel == null) {
            connect(host, port);
        } else if (!channel.isActive()) {
            try {
                channel.close();
            } finally {
                channel = null;
                connect(host, port);
            }
        }
    }


    /**
     * 构造lastInvocationInfo
     *
     * @param info
     * @param respHeader
     */
    private void fillLastInvocationInfo(InvocationInfoImpl info, SoaHeader respHeader) {
        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        info.calleeTid(respHeader.getCallerTid().orElse(0L));
        info.calleeIp(respHeader.getCalleeIp().orElse(0));
        info.calleePort(respHeader.getCalleePort().orElse(0));
        info.calleeMid(respHeader.getCalleeMid().orElse(""));
        info.calleeTime1(respHeader.getCalleeTime1().orElse(0));
        info.calleeTime2(respHeader.getCalleeTime2().orElse(0));
        info.loadBalanceStrategy(invocationContext.loadBalanceStrategy().orElse(null));
        info.responseCode(respHeader.getRespCode().orElse(SoaCode.ClientUnKnown.getCode()));
    }
}
