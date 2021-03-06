package tbcloud.open.httpproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ConfField;
import tbcloud.open.httpproxy.handler.ApikeyHandler;
import tbcloud.open.httpproxy.handler.HttpProxyHandler;

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

    private OpenHttpProxyPlugin plugin;

    HttpProxyServer(OpenHttpProxyPlugin plugin) {
        this.plugin = plugin;
    }

    private EventLoopGroup parentGroup, childGroup;//io thread
    private EventExecutorGroup workGroup, proxyGroup;

    public void start() {
        try {
            String ip = plugin.getConfig(ConfField.OPEN_HTTPPROXY_HOST, "0.0.0.0");
            String port = plugin.getConfig(ConfField.OPEN_HTTPPROXY_PORT, "8108");
            InetSocketAddress addr = new InetSocketAddress(ip, Integer.parseInt(port));

            LOG.info("Start httpproxy server, listen on {}", addr);

            parentGroup = new NioEventLoopGroup(1);
            childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);

            // TODO custom
            workGroup = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 10);
            proxyGroup = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() + 1);

            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup, childGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.WARN)).childHandler(new HttpServerInitializer(workGroup, proxyGroup));
            b.childOption(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true);
            //      .childOption(ChannelOption.SO_LINGER, 10).
            // .option(ChannelOption.ALLOCATOR,PooledByteBufAllocator.DEFAULT)
            ChannelFuture future = b.bind(addr).sync();
            LOG.info("Start httpproxy server successfully!");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
        if (workGroup != null) workGroup.shutdownGracefully();
        if (proxyGroup != null) proxyGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
    }
}

class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private EventExecutorGroup workGroup;
    private EventExecutorGroup proxyGroup;

    public HttpServerInitializer(EventExecutorGroup workGroup, EventExecutorGroup proxyGroup) {
        this.workGroup = workGroup;
        this.proxyGroup = proxyGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        //TODO
        p.addLast(new IdleStateHandler(30, 30, 0));

        p.addLast(new HttpRequestDecoder(4096, 8192, 8192, true));
        p.addLast(new HttpContentDecompressor());

        p.addLast(new HttpResponseEncoder());
//        p.addLast(new HttpContentCompressor()); //TODO HttpContentEncoder 110 cannot send more responses than requests

        p.addLast(workGroup, new ApikeyHandler());
        p.addLast(proxyGroup, new HttpProxyHandler());
    }
}


