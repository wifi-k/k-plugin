package tbcloud.node.httpproxy.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.CharsetUtil;
import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.httpproxy.protocol.data.HttpProxyRequest;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.node.httpproxy.NodeHttpProxyPlugin;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.util.GsonUtil;

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
            int sslEnabled = ((HttpRequest) msg).headers().getInt(ApiConst.HTTPPROXY_SSL);
            int httpPort = ((HttpRequest) msg).headers().getInt(ApiConst.HTTPPROXY_PORT, sslEnabled > 0 ? 443 : 80);
            //int contentLength = ((HttpRequest) msg).headers().getInt(HttpHeaderNames.CONTENT_LENGTH);//body size

            // write header
            ByteBuf buf = ctx.alloc().buffer(1024, PacketConst.MAX_SIZE);
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
            // update offline info
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

    public static final void writeError(ChannelHandlerContext ctx, Result<?> r) {
        String json = GsonUtil.toJson(r);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, code2Status(r.getCode()),
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

//        if (keepAlive) {
//            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        }

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        LOG.info("write error {}", json);
    }

    final static HttpResponseStatus code2Status(int code) {
        switch (code) {
            case ApiCode.SUCC:
                return HttpResponseStatus.OK;
            case ApiCode.INVALID_APIKEY:
                return HttpResponseStatus.NETWORK_AUTHENTICATION_REQUIRED;
            case ApiCode.NODE_NOT_FOUND:
                return HttpResponseStatus.SERVICE_UNAVAILABLE;
            case ApiCode.REQUEST_TIMEOUT:
                return HttpResponseStatus.GATEWAY_TIMEOUT;
            case ApiCode.RESPONSE_TIMEOUT:
                return HttpResponseStatus.GATEWAY_TIMEOUT;
            case ApiCode.IO_ERROR:
                return HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }

        return HttpResponseStatus.BAD_GATEWAY;
    }

}
