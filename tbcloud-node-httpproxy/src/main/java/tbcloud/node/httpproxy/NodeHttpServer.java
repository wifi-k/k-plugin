package tbcloud.node.httpproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ConfField;
import tbcloud.node.httpproxy.http.HttpToTcpHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 接收共享计算的http代理请求,转发到节点
 *
 * @author dzh
 * @date 2018-12-13 23:13
 */
public class NodeHttpServer implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(NodeHttpServer.class);

    private NodeHttpProxyPlugin plugin;

    NodeHttpServer(NodeHttpProxyPlugin plugin) {
        this.plugin = plugin;
    }

    private EventLoopGroup parentGroup, childGroup;//io thread
    private EventExecutorGroup workGroup;

    private String httpIp;
    private int httpPort;

    public void start() {
        try {
            String ip = plugin.getConfig(ConfField.NODE_HTTPPROXY_HTTP_HOST, "0.0.0.0");
            String port = plugin.getConfig(ConfField.NODE_HTTPPROXY_HTTP_HOST, "8109");
            this.httpIp = InetAddress.getLocalHost().getHostAddress();
            this.httpPort = Integer.parseInt(port);
            InetSocketAddress addr = new InetSocketAddress(ip, this.httpPort);

            LOG.info("Starting httpproxy http server, listen on {}", addr);

            parentGroup = new NioEventLoopGroup(1);
            childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);

            // TODO custom
            workGroup = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 10);

            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup, childGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.WARN)).childHandler(new HttpServerInitializer(workGroup));
            b.childOption(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true);

            //      .childOption(ChannelOption.SO_LINGER, 10).
            // .option(ChannelOption.ALLOCATOR,PooledByteBufAllocator.DEFAULT)
            ChannelFuture future = b.bind(addr).sync();
            LOG.info("Start httpproxy http server successfully!");
        } catch (Exception e) {
            LOG.error(e.getMessage());
            channelClosed();
        }
    }

    public int getPort() {
        return this.httpPort;
    }

    public String getIp() {
        return this.httpIp;
    }

    @Override
    public void close() throws IOException {
        channelClosed();
    }

    private void channelClosed() {
        if (parentGroup != null) parentGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
        if (childGroup != null) childGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
        if (workGroup != null) workGroup.shutdownGracefully();
        LOG.info("httpproxy http server closed");
    }
}

class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private EventExecutorGroup workGroup;

    public HttpServerInitializer(EventExecutorGroup workGroup) {
        this.workGroup = workGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        p.addLast(new ReadTimeoutHandler(30));
        p.addLast(new HttpResponseEncoder());
        p.addLast(new HttpContentCompressor());
        p.addLast(new HttpRequestDecoder(4096, 8192, 8192, true));
        p.addLast(new HttpContentDecompressor());
        p.addLast(workGroup, new HttpToTcpHandler());
//        p.addLast(workGroup, new ApikeyHandler());
//        p.addLast(proxyGroup, new HttpProxyHandler());
    }
}


