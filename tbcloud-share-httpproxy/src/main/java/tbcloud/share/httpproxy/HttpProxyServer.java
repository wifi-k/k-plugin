package tbcloud.share.httpproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ConfField;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author dzh
 * @date 2018-12-03 19:29
 */
public class HttpProxyServer implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(HttpProxyServer.class);

    private ShareHttpProxyPlugin plugin;

    HttpProxyServer(ShareHttpProxyPlugin plugin) {
        this.plugin = plugin;
    }

    private EventLoopGroup parentGroup, childGroup;

    public void start() {
        try {
            String ip = plugin.getConfig(ConfField.SHARE_HTTPPROXY_HOST, "0.0.0.0");
            String port = plugin.getConfig(ConfField.SHARE_HTTPPROXY_PORT, "8108");
            InetSocketAddress addr = new InetSocketAddress(ip, Integer.parseInt(port));

            LOG.info("Starting httpproxy server, listen on {}", addr);

            parentGroup = new NioEventLoopGroup(1);
            childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup, childGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.WARN)).childHandler(new HttpServerInitializer());
            b.childOption(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true);
            //      .childOption(ChannelOption.SO_LINGER, 10).
            // .option(ChannelOption.ALLOCATOR,PooledByteBufAllocator.DEFAULT)
            ChannelFuture future = b.bind(addr).sync();
            LOG.info("Start http server successfully!");

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            channelClosed();
        }
    }

    @Override
    public void close() throws IOException {
        channelClosed();
    }

    private void channelClosed() {
        if (parentGroup != null) parentGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
        if (childGroup != null) childGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
    }
}
