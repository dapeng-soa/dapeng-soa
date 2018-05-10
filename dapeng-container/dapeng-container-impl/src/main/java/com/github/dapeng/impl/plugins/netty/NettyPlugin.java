package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.api.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lihuimin
 * @date 2017/12/7
 */
public class NettyPlugin implements AppListener, Plugin {
    private final boolean MONITOR_ENABLE = SoaSystemEnvProperties.SOA_MONITOR_ENABLE;

    private final Container container;

    public NettyPlugin(Container container) {
        this.container = container;
        container.registerAppListener(this);
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(NettyPlugin.class);

    private final int port = SoaSystemEnvProperties.SOA_CONTAINER_PORT;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private ServerBootstrap bootstrap;

    @Override
    public void start() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::start");
        LOGGER.info("Bind Local Port {} [Netty]", port);
        LOGGER.info("ByteBufAllocator:{}", SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ? "pooled" : "unpooled");

        Thread bootstrapThread = new Thread("NettyContainer-Thread") {
            @Override
            public void run() {
                try {
                    bootstrap = new ServerBootstrap();

                    AbstractByteBufAllocator allocator =
                            SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ?
                                    PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;

                    // netty连接数统计
                    NettyConnectCounter channelCounter = new NettyConnectCounter();
                    //流量统计
                    ChannelHandler flowCounter = null;
                    if (MONITOR_ENABLE) flowCounter = new SoaFlowCounter();
                    //编解码器
                    ChannelHandler soaMsgDecoder = new SoaMsgDecoder(container);
                    ChannelHandler soaMsgEncoder = new SoaMsgEncoder(container);
                    //心跳处理
                    ChannelHandler soaIdleHandler = new SoaIdleHandler();
                    //业务处理器
                    ChannelHandler soaServerHandler = new SoaServerHandler(container);
                    ChannelHandler soaFlowCounter = flowCounter;

                    //限流 handler
                    SoaFreqHandler freqHandler = new SoaFreqHandler();

                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    ch.pipeline().addLast(HandlerConstants.SOA_CHANNEL_COUNTER_HANDLER, channelCounter);
                                    // 超时设置
                                    ch.pipeline().addLast(HandlerConstants.IDLE_STATE_HANDLER, new IdleStateHandler(20, 0, 0));
                                    //粘包和断包处理
                                    ch.pipeline().addLast(HandlerConstants.SOA_FRAME_DECODER_HANDLER, new SoaFrameDecoder());
                                    // 流量统计
                                    if (null != soaFlowCounter)
                                        ch.pipeline().addLast(HandlerConstants.SOA_FLOW_COUNTER_HANDLER, soaFlowCounter);
                                    ch.pipeline().addLast(HandlerConstants.SOA_MSG_ENCODER_HANDLER, soaMsgEncoder);
                                    ch.pipeline().addLast(HandlerConstants.SOA_MSG_DECODER_HANDLER, soaMsgDecoder);
                                    // 服务调用统计
                                    if (MONITOR_ENABLE)
                                        ch.pipeline().addLast(HandlerConstants.SOA_INVOKE_COUNTER_HANDLER, new SoaInvokeCounter());
                                    ch.pipeline().addLast(HandlerConstants.SOA_IDLE_HANDLER, soaIdleHandler);
                                    // 添加服务限流handler
                                    ch.pipeline().addLast(HandlerConstants.SOA_FREQ_HANDLER, freqHandler);
                                    ch.pipeline().addLast(HandlerConstants.SOA_SERVER_HANDLER, soaServerHandler);
                                }
                            })
                            .option(ChannelOption.SO_BACKLOG, 1024)
                            .option(ChannelOption.ALLOCATOR, allocator)
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            .childOption(ChannelOption.ALLOCATOR, allocator);

                    // Start the server.
                    ChannelFuture f = bootstrap.bind(port).sync();

                    // Wait until the connection is closed.
                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                }
            }
        };
        bootstrapThread.setDaemon(true);
        bootstrapThread.start();
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::" + getClass().getSimpleName() + "::stop");
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    @Override
    public void appRegistered(AppEvent event) {
        LOGGER.info(getClass().getSimpleName() + "::appRegistered event:[" + event.getSource() + "]");
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        LOGGER.info(getClass().getSimpleName() + "::appUnRegistered event:[" + event.getSource() + "]");
    }
}
