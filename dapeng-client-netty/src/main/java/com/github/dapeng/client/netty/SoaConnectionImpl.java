package com.github.dapeng.client.netty;

import com.github.dapeng.util.SoaMessageBuilder;
import com.github.dapeng.util.SoaMessageParser;
import com.github.dapeng.client.filter.LoadBalanceFilter;
import com.github.dapeng.client.json.JsonSerializer;
import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.*;
import com.github.dapeng.util.SoaMessageBuilder;
import com.github.dapeng.util.SoaMessageParser;
import com.github.dapeng.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SoaConnectionImpl implements SoaConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);

    private final String host;
    private final int port;
    private Channel channel = null;
    private NettyClient client; // Netty Channel
    private AtomicInteger seqidAtomic = new AtomicInteger(0);


    public SoaConnectionImpl(String host, int port) {
        this.client = NettyClientFactory.getNettyClient();
        this.host = host;
        this.port = port;
        try {
            channel = connect(host,port);
        } catch (Exception e) {
            LOGGER.error("connect to {}:{} failed", host, port);
        }
    }


    @Override
    public <REQ, RESP> RESP send(
            String service, String version, String method,
            REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws SoaException {

        // InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();

        int seqid = this.seqidAtomic.getAndIncrement();

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException{

                    ByteBuf requestBuf = buildRequestBuf(service, version, method, seqid, request, requestSerializer);

                    // TODO filter
                    checkChannel();
                    ByteBuf responseBuf = client.send(channel,seqid, requestBuf); //发送请求，返回结果
                    processResponse(responseBuf,responseSerializer,ctx,this);
                    onExit(ctx, getPrevChain(ctx));
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev)  {

            }
        };

        SharedChain sharedChain = new SharedChain(new LoadBalanceFilter(), new ArrayList<>(), dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        try {
            sharedChain.onEntry(filterContext);
        } catch (TException e) {
            throw new SoaException(e);
        }
        RESP response = (RESP) filterContext.getAttach(dispatchFilter,"response");

        return response;
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer,
                                              BeanSerializer<RESP> responseSerializer, long timeout) throws SoaException {

        int seqid = this.seqidAtomic.getAndIncrement();

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) {
                try {

                    ByteBuf requestBuf = buildRequestBuf(service, version, method, seqid, request, requestSerializer);

                    CompletableFuture<ByteBuf> responseBufFuture = new CompletableFuture<>();
                    try {
                        checkChannel();
                        client.sendAsync(channel, seqid, requestBuf, responseBufFuture, timeout); //发送请求，返回结果
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    CompletableFuture<RESP> responseFuture = responseBufFuture.thenApply((ByteBuf responseBuf) -> {
                        try {
                            // TODO fill InvocationContext.lastInfo from response.Header
                            RESP result = processResponse(responseBuf,responseSerializer,ctx,this);
                            onExit(ctx, getPrevChain(ctx));
                            return result;
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(),e);
                            return null;
                        }
                    });
                    ctx.setAttach(this, "response", responseFuture);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev)  {

            }
        };

        SharedChain sharedChain = new SharedChain(new LoadBalanceFilter(), new ArrayList<>(), dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        try {
            sharedChain.onEntry(filterContext);
        } catch (TException e) {
            throw new SoaException(e);
        }
        CompletableFuture<RESP> resultFuture = (CompletableFuture<RESP>) filterContext.getAttach(dispatchFilter,"response");

        return resultFuture;
    }

    public <REQ, RESP> RESP sendJson(REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws SoaException {
int seqid = this.seqidAtomic.getAndIncrement();

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException{

                ByteBuf requestBuf = buildJsonRequestBuf(seqid, request, requestSerializer);

                // TODO filter
                checkChannel();
                ByteBuf responseBuf = client.send(channel,seqid, requestBuf); //发送请求，返回结果
                ((JsonSerializer)responseSerializer).setByteBuf(responseBuf);
                processResponse(responseBuf,responseSerializer,ctx,this);
                onExit(ctx, getPrevChain(ctx));
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev)  {

            }
        };

        SharedChain sharedChain = new SharedChain(new LoadBalanceFilter(), new ArrayList<>(), dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        try {
            sharedChain.onEntry(filterContext);
        } catch (TException e) {
            throw new SoaException(e);
        }
        RESP response = (RESP) filterContext.getAttach(dispatchFilter,"response");

        return response;
    }

    private SoaHeader buildHeader(String service, String version, String method) {
        SoaHeader header = new SoaHeader();
        header.setServiceName(service);
        header.setVersionName(version);
        header.setMethodName(method);

        // TODO fill header from InvocationContext

        return header;
    }

    private <REQ> ByteBuf buildRequestBuf(String service, String version, String method,int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);//Unpooled.directBuffer(8192);  // TODO Pooled

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        // TODO set protocol
        SoaHeader header = buildHeader(service, version, method);
        try {
            ByteBuf buf = builder.buffer(requestBuf)
                    .header(header)
                    .body(request, requestSerializer)
                    .seqid(seqid)
                    .build();
            return buf;
        } catch (TException e) {
            throw new SoaException(e);
        }
    }

    private <REQ> ByteBuf buildJsonRequestBuf(int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);//Unpooled.directBuffer(8192);  // TODO Pooled

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        ((JsonSerializer)requestSerializer).setByteBuf(requestBuf);
        try {
            ByteBuf buf = builder.buffer(requestBuf)
                    .body(request, requestSerializer)
                    .seqid(seqid)
                    .buildJson();
            return buf;
        } catch (TException e) {
            throw new SoaException(e);
        }
    }

    private <RESP> RESP processResponse(ByteBuf responseBuf,BeanSerializer<RESP> responseSerializer,FilterContext ctx,Filter filter) throws SoaException{
       try {
           if (responseBuf == null) {
               throw new SoaException(SoaBaseCode.TimeOut);
           } else {
               SoaMessageParser parser = new SoaMessageParser(responseBuf, responseSerializer).parseHeader();
               // TODO fill InvocationContext.lastInfo from response.Header
               SoaHeader respHeader = parser.getHeader();

               if ("0000".equals(respHeader.getRespCode().get())) {
                   parser.parseBody();
                   RESP resp = (RESP) parser.getBody();
                   ctx.setAttach(filter, "response", resp);
                   return resp;
               } else {
                   throw new SoaException((respHeader.getRespCode().isPresent())?respHeader.getRespCode().get():SoaBaseCode.UnKnown.getCode()  , (respHeader.getRespMessage().isPresent())?respHeader.getRespMessage().get():SoaBaseCode.UnKnown.getMsg());
               }

           }
       }catch (TException e){
           throw new SoaException(e);
       }finally {
           responseBuf.release();
       }

    }

    /**
     * 创建连接
     *
     */
    private synchronized Channel connect(String host, int port) throws SoaException {
        if (channel != null && channel.isActive())
            return channel;

        try {
            return channel = this.client.getBootstrap().connect(host, port).sync().channel();
        } catch (Exception e) {
            throw new SoaException(SoaBaseCode.NotConnected);
        }
    }

    private void checkChannel() throws SoaException {
        if (channel == null || !channel.isActive())
            connect(host, port);
    }

}
