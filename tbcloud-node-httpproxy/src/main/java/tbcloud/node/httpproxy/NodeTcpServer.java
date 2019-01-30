package tbcloud.node.httpproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ConfField;
import tbcloud.node.httpproxy.tcp.HttpProxyDataHandler;
import tbcloud.node.httpproxy.tcp.HttpProxyDecoder;
import tbcloud.node.httpproxy.tcp.HttpProxyEncoder;
import tbcloud.node.httpproxy.tcp.HttpProxyRequestEncoder;
import tbcloud.node.protocol.ProtocolConst;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 和节点的TCP连接
 *
 * @author dzh
 * @date 2018-12-03 19:32
 */

public class NodeTcpServer implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(NodeTcpServer.class);

    private EventLoopGroup parentGroup, childGroup;//io thread
    private EventExecutorGroup workGroup; // TODO split by DataType

    private NodeHttpProxyPlugin plugin;

    public NodeTcpServer(NodeHttpProxyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            String ip = plugin.getConfig(ConfField.NODE_HTTPPROXY_TCP_HOST, "0.0.0.0");
            String port = plugin.getConfig(ConfField.NODE_HTTPPROXY_TCP_PORT, "9109");
            InetSocketAddress addr = new InetSocketAddress(ip, Integer.parseInt(port));

            LOG.info("Starting httpproxy tcp server, listen on {}", addr);

            parentGroup = new NioEventLoopGroup(1);
            childGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 1);

            // TODO custom
            workGroup = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 20);

            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup, childGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.WARN)).childHandler(new TcpServerInitializer(workGroup));
            b.childOption(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true);
            //      .childOption(ChannelOption.SO_LINGER, 10).
            // .option(ChannelOption.ALLOCATOR,PooledByteBufAllocator.DEFAULT)
            ChannelFuture future = b.bind(addr).sync();
            LOG.info("Start httpproxy tcp server successfully!");
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
        LOG.info("httpproxy tcp server closed");
    }
}

class TcpServerInitializer extends ChannelInitializer<SocketChannel> {

    private EventExecutorGroup workGroup;

    public TcpServerInitializer(EventExecutorGroup workGroup) {
        this.workGroup = workGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        // TODO 更新节点的离线信息
        //p.addLast(new ReadTimeoutHandler(ProtocolConst.HEARTBEAT_TICK * 5)); //5个心跳超时
        int timeout = ProtocolConst.HEARTBEAT_TICK * 5;
        p.addLast(new IdleStateHandler(timeout, timeout, 0));
        p.addLast(new HttpProxyEncoder());
        p.addLast(new HttpProxyRequestEncoder());
        p.addLast(new HttpProxyDecoder());
        p.addLast(workGroup, new HttpProxyDataHandler());

    }
}
