import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-12-18 9:50 AM
 */
public class EchoServer {

    private final int port;
    private List<ChannelFuture> channelFutures = new ArrayList<ChannelFuture>(2);

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(4);

        for (int i = 0; i != 2; ++i) {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // the channel type
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            System.out.println("Connection accepted by server");
                            ch.pipeline().addLast(
                                    new EchoServerHandler());
                        }
                    });
            // wait till binding to port completes
            ChannelFuture f = b.bind(port + i).sync();
            channelFutures.add(f);
            System.out.println("Echo server started and listen on " + f.channel().localAddress());
        }

        for (ChannelFuture f : channelFutures)
            f.channel().closeFuture().sync();

        // close gracefully
        workerGroup.shutdownGracefully().sync();
        bossGroup.shutdownGracefully().sync();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + EchoServer.class.getSimpleName() +
                            " <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        new EchoServer(port).start();
    }
}
