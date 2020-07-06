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

import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by lihuimin on 2017/12/21.
 */
public class NettyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);

    private final int readerIdleTimeSeconds = 45;
    private final int writerIdleTimeSeconds = 15;
    private final int allIdleTimeSeconds = 0;

    private Bootstrap bootstrap = null;
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    private static class RequestQueue {

        private static class AsyncRequestWithTimeout {
            public AsyncRequestWithTimeout(int seqid, long timeout, CompletableFuture future) {
                this.seqid = seqid;
                this.expired = System.currentTimeMillis() + timeout;
                this.future = future;
            }

            final long expired;
            final int seqid;
            final CompletableFuture<?> future;
        }

        private static final Map<Integer, CompletableFuture<ByteBuf>> FUTURE_CACHES =
                new ConcurrentHashMap<>();
        private static final PriorityBlockingQueue<AsyncRequestWithTimeout> FUTURES_CACHES_WITH_TIMEOUT =
                new PriorityBlockingQueue<>(256,
                        (o1, o2) -> (int) (o1.expired - o2.expired));

        static void put(int seqId, CompletableFuture<ByteBuf> requestFuture) {
            FUTURE_CACHES.put(seqId, requestFuture);
        }

        static void putAsync(int seqId, CompletableFuture<ByteBuf> requestFuture, long timeout) {
            FUTURE_CACHES.put(seqId, requestFuture);

            AsyncRequestWithTimeout fwt = new AsyncRequestWithTimeout(seqId, timeout, requestFuture);
            FUTURES_CACHES_WITH_TIMEOUT.add(fwt);
        }

        /*
         * block1 in discard, and block in complete will be synchornized, so
         * 1. block1 then block2
         *    - block2's future2 will be null, so the ByteBuf will released
         * 2. block2 then block1
         *    - block1 will get the msg and release it
         * 3. No block1, just block2
         *    - future.complete(msg) success.
         * 4. No block2, Just block1
         *    - the future not completed, simple discard it from queue
         */

        static CompletableFuture<ByteBuf> discard(int seqId) {
            CompletableFuture<ByteBuf> future = FUTURE_CACHES.get(seqId);
            if(future != null){
                block1: synchronized (future) {  // complete(seqid, msg) maybe execute before here,
                    ByteBuf msg = future.getNow(null);
                    if (msg != null) msg.release();
                    FUTURE_CACHES.remove(seqId);
                }
            }
            return future;
        }

        static void complete(int seqid, ByteBuf msg) {
            CompletableFuture<ByteBuf> future = FUTURE_CACHES.get(seqid);
            if(future != null){
                block2: synchronized (future) { // discard(seqId) maybe execute before here
                    CompletableFuture<ByteBuf> future2 = FUTURE_CACHES.get(seqid);
                    if(future2 != null)
                        future2.complete(msg);
                    else
                        msg.release();

                    FUTURE_CACHES.remove(seqid);
                }
            }
            else {
                LOGGER.error("返回结果超时，siqid为：" + seqid);
                msg.release();
            }
        }

        /**
         * 一次检查中超过50个请求超时就打印一下日志
         */
        static void checkTimeout() {
            long now = System.currentTimeMillis();

            AsyncRequestWithTimeout fwt = FUTURES_CACHES_WITH_TIMEOUT.peek();
            while (fwt != null && fwt.expired < now) {
                CompletableFuture future = fwt.future;
                if (future.isDone() == false) {
                    future.completeExceptionally(new SoaException(SoaCode.ReqTimeOut));
                }

                FUTURES_CACHES_WITH_TIMEOUT.remove();
                discard(fwt.seqid);

                fwt = FUTURES_CACHES_WITH_TIMEOUT.peek();
            }
        }
    }

    public NettyClient() {
        initBootstrap();
    }

    protected Bootstrap initBootstrap() {
        AbstractByteBufAllocator allocator =
                SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ?
                        PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, allocator);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds),
                        new SoaFrameDecoder(), //粘包和断包处理
                        new SoaIdleHandler(),
                        new SoaClientHandler(callBack));
            }
        });
        return bootstrap;
    }

    /**
     * @param channel
     * @param seqid
     * @param request
     * @param timeout
     * @param service 传入 service 参数 是为了返回服务超时信息更具体
     * @return
     * @throws SoaException
     */
    public ByteBuf send(Channel channel, int seqid, ByteBuf request, long timeout, String service) throws SoaException {

        //means that this channel is not idle and would not managered by IdleConnectionManager
        IdleConnectionManager.remove(channel);

        CompletableFuture<ByteBuf> future = new CompletableFuture<>();

        RequestQueue.put(seqid, future);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NettyClient::send, timeout:" + timeout + ", seqId:" + seqid + ",  to: " + channel.remoteAddress());
        }

        try {
            channel.writeAndFlush(request);
            ByteBuf respByteBuf = future.get(timeout, TimeUnit.MILLISECONDS);
            return respByteBuf;
        } catch (TimeoutException e) {
            // 如果在服务里面, 那么不清理MDC
            if (!TransactionContext.hasCurrentInstance()) {
                MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            }
            LOGGER.error("请求服务超时[" + service + "]，seqid:" + seqid);
            throw new SoaException(SoaCode.ReqTimeOut.getCode(), "请求服务超时[" + service + "]");
        } catch (Throwable e) {
            // 如果在服务里面, 那么不清理MDC
            if (!TransactionContext.hasCurrentInstance()) {
                MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            }
            throw new SoaException(SoaCode.ClientUnKnown, e.getMessage() == null ? SoaCode.ClientUnKnown.getMsg() : e.getMessage());
        } finally {
            RequestQueue.discard(seqid);
        }

    }

    public CompletableFuture<ByteBuf> sendAsync(Channel channel, int seqid, ByteBuf request, long timeout) throws Exception {

        IdleConnectionManager.remove(channel);

        CompletableFuture<ByteBuf> future = new CompletableFuture<>();

        RequestQueue.putAsync(seqid, future, timeout);

        channel.writeAndFlush(request);

        return future;
    }

    private SoaClientHandler.CallBack callBack = msg -> {
        // length(4) stx(1) version(1) protocol(1) seqid(4) header(...) body(...) etx(1)
        int readerIndex = msg.readerIndex();
        msg.skipBytes(7); // length4 + stx1 + version1 + protocol1
        int seqid = msg.readInt();

        msg.readerIndex(readerIndex);
        RequestQueue.complete(seqid, msg);
    };

    /**
     * 定时任务，使得超时的异步任务返回异常给调用者
     */
    private static long DEFAULT_SLEEP_TIME = 100L;

    static {

        final Thread asyncCheckTimeThread = new Thread("ConnectionPool-ReqTimeout-Thread") {
            @Override
            public void run() {
                while (true) {
                    try {
                        RequestQueue.checkTimeout();
                        Thread.sleep(DEFAULT_SLEEP_TIME);
                    } catch (Exception e) {
                        LOGGER.error("Check Async Timeout Thread Error", e);
                    }
                }
            }
        };
        asyncCheckTimeThread.start();
    }


    /**
     * 同步连接并返回channel
     *
     * @param host
     * @param port
     * @return
     * @throws InterruptedException
     */
    public Channel connect(String host, int port) throws InterruptedException {
        return bootstrap.connect(host, port).sync().channel();
    }

    public void shutdown() {
        LOGGER.warn("NettyClient shutdown gracefully");
        workerGroup.shutdownGracefully();
    }

}
