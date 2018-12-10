package tbcloud.share.httpproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import tbcloud.share.httpproxy.handler.ApikeyHandler;
import tbcloud.share.httpproxy.handler.HttpProxyHandler;

/**
 * @author dzh
 * @date 2018-12-03 19:37
 */
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
        p.addLast(new ReadTimeoutHandler(10));
        p.addLast(new HttpRequestDecoder(4096, 8192, 8192, true));
        p.addLast(new HttpContentDecompressor());
        p.addLast(new HttpResponseEncoder());
        p.addLast(new HttpContentCompressor());
        p.addLast(workGroup, new ApikeyHandler());
        p.addLast(proxyGroup, new HttpProxyHandler());
    }
}
