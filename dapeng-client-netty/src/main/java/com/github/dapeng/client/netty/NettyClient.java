package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;
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

        static CompletableFuture<ByteBuf> remove(int seqId) {
            return FUTURE_CACHES.remove(seqId);
            // remove from prior-queue
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
                    future.completeExceptionally(new SoaException(SoaCode.TimeOut));
                }

                FUTURES_CACHES_WITH_TIMEOUT.remove();
                remove(fwt.seqid);

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
            LOGGER.error("请求服务超时[" + service + "]，seqid:" + seqid);
            throw new SoaException(SoaCode.TimeOut.getCode(), "请求服务超时[" + service + "]");
        } catch (Throwable e) {
            throw new SoaException(SoaCode.UnKnown, e.getMessage() == null ? SoaCode.UnKnown.getMsg() : e.getMessage());
        } finally {
            RequestQueue.remove(seqid);
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

        CompletableFuture<ByteBuf> future = RequestQueue.remove(seqid);
        if (future != null) {
            future.complete(msg); // released in ...
        } else {
            LOGGER.error("返回结果超时，siqid为：" + seqid);
            msg.release();
        }
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

}
