package tbcloud.share.httpproxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.CharsetUtil;
import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.httpproxy.dao.service.HttpProxyDaoService;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.select.service.NodeSelectService;
import tbcloud.share.httpproxy.ShareHttpProxyPlugin;
import tbcloud.user.dao.service.UserDaoService;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author dzh
 * @date 2018-12-07 10:01
 */
@Injector
public abstract class AbstractInboundHandler extends SimpleChannelInboundHandler<HttpObject> {

    static Logger LOG = LoggerFactory.getLogger(AbstractInboundHandler.class);

    @InjectPlugin
    protected static ShareHttpProxyPlugin Plugin;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @InjectService(id = "tbcloud.service.user.dao")
    static UserDaoService UserDao;

    @InjectService(id = "tbcloud.service.httpproxy.dao")
    static HttpProxyDaoService HttpProxyDao;

    @InjectService(id = "tbcloud.service.node.select")
    static NodeSelectService NodeSelector;

    public static final void writeError(ChannelHandlerContext ctx, boolean keepAlive, String cookieString, Result<?> r) {
        String json = GsonUtil.toJson(r);
        LOG.info("write error {}", json);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, newStatus(r.getCode()),
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        //String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
            if (!cookies.isEmpty()) {  // Reset the cookies if necessary.
                for (Cookie cookie : cookies) {
                    response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
                }
            }
        } else { // Browser sent no cookie.  Add some.
            // response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode("key1", "value1"));
        }

        ctx.writeAndFlush(response);

        if (!keepAlive) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    static HttpResponseStatus newStatus(int code) {
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

    protected Result<Void> newResult(int code, String msg) {
        Result<Void> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    protected String getFromRedis(String id, String key) {
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                return jedis.get(key);
            }
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Type type) {
        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                String json = jedis.get(key);
                if (!StringUtil.isEmpty(json)) {
                    return GsonUtil.<T>fromJson(json, type);
                }
            }
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Class<T> clazz) {
        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                String json = jedis.get(key);
                if (!StringUtil.isEmpty(json)) {
                    return GsonUtil.<T>fromJson(json, clazz);
                }
            }
        }
        return null;
    }

    public void setToRedis(String id, String key, String value, Integer expiredSeconds) {
        if (value == null) return;

        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                jedis.setex(key, expiredSeconds, value);
            }
        }
    }

    public void deleteFromRedis(String id, String key) {
        if (StringUtil.isEmpty(key)) return;

        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                jedis.del(key);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error(cause.getMessage(), cause);

        if (cause instanceof ReadTimeoutException) {
            // TODO
        } else {
            if (ctx.channel().isActive()) {
                Result<Void> r = new Result<>();
                r.setCode(ApiCode.ERROR_UNKNOWN);
                r.setMsg(cause.getMessage());
                writeError(ctx, false, null, r); //close channel
            } else {
                ctx.close();
            }
        }

    }

}
