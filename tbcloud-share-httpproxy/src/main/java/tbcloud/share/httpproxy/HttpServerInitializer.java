package tbcloud.share.httpproxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * @author dzh
 * @date 2018-12-03 19:37
 */
class HttpServerInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        p.addLast(new ReadTimeoutHandler(10)); //TODO
        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpResponseEncoder());
        p.addLast(new HttpContentCompressor());



    }
}
