package tbcloud.node.httpproxy.dispatch;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.dao.service.HttpProxyDaoService;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.node.protocol.util.GsonUtil;

/**
 * @author dzh
 * @date 2018-12-16 17:56
 */
@Injector
public class DispatchRecord implements AutoCloseable {

    static Logger LOG = LoggerFactory.getLogger(DispatchRecord.class);

    private String id; //record id
    private ChannelHandlerContext httpContext;

    @InjectService(id = "tbcloud.service.httpproxy.dao")
    static HttpProxyDaoService HttpProxyDao;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ChannelHandlerContext getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(ChannelHandlerContext httpContext) {
        this.httpContext = httpContext;
    }

    @Override
    public void close() throws Exception {
        if (httpContext != null)
            httpContext.channel().close();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DispatchRecord) {
            return ((DispatchRecord) obj).getId().equals(id);
        }
        return false;
    }

    public final void writeError(Result<?> r) {
        String json = GsonUtil.toJson(r);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, code2Status(r.getCode()),
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(ApiConst.HTTPPROXY_ERROR_CODE, r.getCode());
        response.headers().set(ApiConst.HTTPPROXY_ERROR_MSG, r.getMsg());

        if (httpContext != null) {
            httpContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            LOG.info("write error {}", json);
        }
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
