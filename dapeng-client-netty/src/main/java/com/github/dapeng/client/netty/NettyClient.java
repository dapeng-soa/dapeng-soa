package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaBaseCode;
import com.github.dapeng.core.SoaException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
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
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by lihuimin on 2017/12/21.
 */
public class NettyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);

    private final int readerIdleTimeSeconds = 15;
    private final int writerIdleTimeSeconds = 10;
    private final int allIdleTimeSeconds = 0;

    private Bootstrap bootstrap = null;
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    private static final Map<Integer, CompletableFuture> futureCaches = new ConcurrentHashMap<>();

    private static final Queue<AsyncRequestWithTimeout> futuresCachesWithTimeout = new PriorityQueue<>((o1, o2) -> (int) (o1.getTimeout() - o2.getTimeout()));

    public NettyClient(){
        initBootstrap();
    }

    protected Bootstrap initBootstrap() {
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds), new SoaDecoder(), new SoaIdleHandler(),new SoaClientHandler(callBack));
            }
        });
        return bootstrap;
    }

    public ByteBuf send(Channel channel ,int seqid, ByteBuf request) throws SoaException {

        //means that this channel is not idle and would not managered by IdleConnectionManager
        IdleConnectionManager.remove(channel);

        CompletableFuture<ByteBuf> future = new CompletableFuture<>();

        futureCaches.put(seqid, future);

        try {
            channel.writeAndFlush(request);
            ByteBuf respByteBuf = future.get(30000, TimeUnit.MILLISECONDS);
            return respByteBuf;
        }catch (Exception e){
            throw new SoaException(SoaBaseCode.UnKnown,e.getMessage());
        }finally {
            futureCaches.remove(seqid);
        }

    }

    public void sendAsync(Channel channel,int seqid, ByteBuf request, CompletableFuture<ByteBuf> future, long timeout) throws Exception {

        IdleConnectionManager.remove(channel);
        futureCaches.put(seqid, future);

        AsyncRequestWithTimeout fwt = new AsyncRequestWithTimeout(seqid, timeout, future);
        futuresCachesWithTimeout.add(fwt);

        channel.writeAndFlush(request);
    }

    private SoaClientHandler.CallBack callBack = msg -> {
        // length(4) stx(1) version(...) protocol(1) seqid(4) header(...) body(...) etx(1)
        int readerIndex = msg.readerIndex();
        msg.skipBytes(7);
        int seqid = msg.readInt();

        msg.readerIndex(readerIndex);

        if (futureCaches.containsKey(seqid)) {
            CompletableFuture<ByteBuf> future = (CompletableFuture<ByteBuf>) futureCaches.get(seqid);
            future.complete(msg);
        }else{

            LOGGER.error("返回结果超时，siqid为：" + String.valueOf(seqid));
            msg.release();
        }
    };

    /**
     * 定时任务，使得超时的异步任务返回异常给调用者
     */
    private static long DEFAULT_SLEEP_TIME = 1000L;

    static {

        final Thread asyncCheckTimeThread = new Thread("Check Async Timeout Thread") {
            @Override
            public void run() {
                while (true) {
                    try {
                        checkAsyncTimeout();
                    } catch (Exception e) {
                        LOGGER.error("Check Async Timeout Thread Error", e);
                    }
                }
            }
        };
        asyncCheckTimeThread.start();
    }

    private static void checkAsyncTimeout() throws InterruptedException {

        AsyncRequestWithTimeout fwt = futuresCachesWithTimeout.peek();

        while (fwt != null && fwt.getTimeout() < System.currentTimeMillis()) {
            LOGGER.info("异步任务({})超时...", fwt.getSeqid());
            futuresCachesWithTimeout.remove();

            CompletableFuture future = futureCaches.get(fwt.getSeqid());
            future.completeExceptionally(new SoaException(SoaBaseCode.TimeOut));
            futureCaches.remove(fwt.getSeqid());

            fwt = futuresCachesWithTimeout.peek();
        }
        Thread.sleep(DEFAULT_SLEEP_TIME);
    }


    public Bootstrap getBootstrap (){
        return bootstrap;
    }

}
