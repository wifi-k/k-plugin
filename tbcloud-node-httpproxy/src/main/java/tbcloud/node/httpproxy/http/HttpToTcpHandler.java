package tbcloud.node.httpproxy.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.httpproxy.protocol.data.HttpProxyRequest;
import tbcloud.httpproxy.protocol.data.HttpProxyResponse;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.node.httpproxy.NodeHttpProxyPlugin;
import tbcloud.node.httpproxy.dispatch.DispatchNode;
import tbcloud.node.httpproxy.dispatch.DispatchRecord;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.util.GsonUtil;

import java.net.URI;
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

    private String recordId;
    private String nodeId;

    private short seqNum = 1;

    private boolean succ_w = true; //write status

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        //LOG.info("nodes {}", Plugin.dispatch());
        if (msg instanceof HttpRequest) {
            this.nodeId = ((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_NODE);
            this.recordId = ((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_RECORD);

            // clear header
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_NODE);
            ((HttpRequest) msg).headers().remove(ApiConst.HTTPPROXY_RECORD);
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
            URI uri = URI.create(((HttpRequest) msg).uri());
            HttpProxyRequest request = new HttpProxyRequest();
            request.setNodeId(nodeId);
            request.setId(recordId);
            request.setScheme("https".equals(uri.getScheme()) ? HttpProxyConst.SCHEME_HTTPS : HttpProxyConst.SCHEME_HTTP);
            request.setHost(uri.getHost());
            request.setPort(uri.getPort() < 0 ? 80 : uri.getPort());
            request.setSeq(seqNum);
            request.setHttp(ByteBuffer.wrap(buf.array(), 0, buf.readableBytes()));

            buf.clear();
            succ_w = write(ctx, request);
        } else if (succ_w && msg instanceof HttpContent) {
            HttpProxyRequest request = new HttpProxyRequest();
            request.setNodeId(nodeId);
            request.setId(recordId);
            if (msg instanceof LastHttpContent) {
                request.setSeq(HttpProxyConst.SEQ_LAST_NUM);
            } else {
                request.setSeq(++seqNum);
            }

            ByteBuf buf = ((HttpContent) msg).content();
            request.setHttp(ByteBuffer.wrap(buf.array(), 0, buf.readableBytes()));
            succ_w = write(ctx, request);
        }
    }

    final boolean write(ChannelHandlerContext ctx, HttpProxyRequest request) {
        DispatchNode dispatchNode = Plugin.dispatch().findNode(request.getNodeId());
        if (dispatchNode == null) {
            Result<Void> r = new Result<>();
            r.setCode(ApiCode.NODE_NOT_FOUND);
            r.setMsg("not found proxy node " + request.getNodeId());
            writeError(ctx, r);
            return false;
        } else {
            dispatchNode.writeRequest(ctx, request);
        }
        return true;
    }

    final void writeError(ChannelHandlerContext ctx, Result<?> r) {
        String json = GsonUtil.toJson(r);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, DispatchRecord.code2Status(r.getCode()),
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(ApiConst.HTTPPROXY_ERROR_CODE, r.getCode());
        response.headers().set(ApiConst.HTTPPROXY_ERROR_MSG, r.getMsg());

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        LOG.info("write error {}", json);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error(cause.getMessage(), cause);

        HttpProxyResponse response = new HttpProxyResponse();
        response.setId(recordId);
        response.setProxyStatus(HttpProxyConst.PROXY_STATUS_FAIL);
        Plugin.dispatch().findNode(nodeId).writeResponse(response);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) { //timeout
            HttpProxyResponse response = new HttpProxyResponse();
            response.setId(recordId);
            response.setProxyStatus(HttpProxyConst.PROXY_STATUS_TIMEOUT);

            Plugin.dispatch().findNode(nodeId).writeResponse(response);
        }
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
