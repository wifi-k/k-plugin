package tbcloud.node.httpproxy.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutException;
import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.httpproxy.protocol.data.HttpProxyRequest;
import tbcloud.lib.api.ApiConst;
import tbcloud.node.httpproxy.NodeHttpProxyPlugin;
import tbcloud.node.protocol.PacketConst;

import java.nio.ByteBuffer;

import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;

/**
 * @author dzh
 * @date 2018-12-16 17:22
 */
@Injector
public class HttpToTcpHandler extends SimpleChannelInboundHandler<HttpObject> {

    static Logger LOG = LoggerFactory.getLogger(HttpToTcpHandler.class);

    private static final byte[] CRLF = {CR, LF};
    static HttpRequestEncoder_EXT HttpRequestEncoderExt = new HttpRequestEncoder_EXT();

    @InjectPlugin
    static NodeHttpProxyPlugin Plugin;

    private HttpProxyRequest request;

    private short seqNum = 1;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            String nodeId = ((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_NODE);
            String recordId = ((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_RECORD);
            int sslEnabled = ((HttpRequest) msg).headers().getInt(ApiConst.HTTPPROXY_SSL, 0);
            int httpPort = ((HttpRequest) msg).headers().getInt(ApiConst.HTTPPROXY_PORT, sslEnabled > 0 ? 443 : 80);

            // clear header
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_NODE);
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_RECORD);
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_SSL);
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_PORT);
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_POLICY);
            ((HttpRequest) msg).headers().remove(ApiConst.API_APIKEY);
            ((HttpRequest) msg).headers().remove(ApiConst.API_VERSION);
            ((HttpRequest) msg).headers().remove(ApiConst.API_TOKEN);

            // write header
            ByteBuf buf = ctx.alloc().heapBuffer(1024, PacketConst.MAX_SIZE);
            // Encode the message.
            HttpRequestEncoderExt.encodeInitialLine(buf, (HttpRequest) msg);
            HttpRequestEncoderExt.encodeHeaders(((HttpRequest) msg).headers(), buf);
            buf.writeBytes(CRLF);

            //
            this.request = new HttpProxyRequest();
            request.setNodeId(nodeId);
            request.setId(recordId);
            request.setHost(((HttpRequest) msg).uri());
            request.setScheme(sslEnabled > 0 ? HttpProxyConst.SCHEME_HTTPS : HttpProxyConst.SCHEME_HTTP);
            request.setPort(httpPort);
            request.setSeq(seqNum);
            request.setHttp(ByteBuffer.wrap(buf.array(), 0, buf.readableBytes()));

            buf.clear();
            Plugin.dispatch().findNode(request.getNodeId()).writeRequest(ctx, request);
        } else if (msg instanceof HttpContent) {
            if (msg instanceof LastHttpContent) {
                request.setSeq(HttpProxyConst.SEQ_LAST_NUM);
            } else {
                request.setSeq(++seqNum);
            }
            ByteBuf buf = ((HttpContent) msg).content();
            request.setHttp(ByteBuffer.wrap(buf.array(), 0, buf.readableBytes()));
            Plugin.dispatch().findNode(request.getNodeId()).writeRequest(ctx, request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error(cause.getMessage(), cause);
        if (cause instanceof ReadTimeoutException) {
        }
        // close
        ctx.channel().close();
        // clear cache
        Plugin.dispatch().findNode(request.getNodeId()).removeDispatchRecord(request.getId());
    }

    static class HttpRequestEncoder_EXT extends HttpRequestEncoder {
        @Override
        public void encodeInitialLine(ByteBuf buf, HttpRequest request) throws Exception {
            super.encodeInitialLine(buf, request);
        }

        public void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
            super.encodeHeaders(headers, buf);
        }
    }

}
