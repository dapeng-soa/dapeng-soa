package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.api.AppListener;
import com.github.dapeng.api.Container;
import com.github.dapeng.core.Plugin;
import com.github.dapeng.api.events.AppEvent;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date 2017/12/7
 * @author lihuimin
 */
public class NettyPlugin implements AppListener, Plugin {

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
        LOGGER.warn("Plugin::NettyPlugin start");
        LOGGER.info("Bind Local Port {} [Netty]", port);
        LOGGER.info("ByteBufAllocator:{}", SoaSystemEnvProperties.SOA_POOLED_BYTEBUF?"pooled":"unpooled");

        new Thread("NettyContainer-Thread") {
            @Override
            public void run() {
                try {
                    bootstrap = new ServerBootstrap();

                    AbstractByteBufAllocator allocator =
                            SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ?
                                    PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;

                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    ch.pipeline().addLast(new IdleStateHandler(15, 0, 0), //超时设置
                                            new SoaDecoder(), //粘包和断包处理
                                            new SoaIdleHandler(),  //心跳处理
                                            new SoaServerHandler(container));  //调用处理
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
        }.start();
    }

    @Override
    public void stop() {
        LOGGER.warn("Plugin::NettyPlugin stop");
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    @Override
    public void appRegistered(AppEvent event) {

    }

    @Override
    public void appUnRegistered(AppEvent event) {

    }
}
