package tbcloud.share.httpproxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;

/**
 * @author dzh
 * @date 2018-12-06 20:11
 */
public class HttpProxyHandler extends AbstractInboundHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // gen proxyid

        // pub mq
    }
}
