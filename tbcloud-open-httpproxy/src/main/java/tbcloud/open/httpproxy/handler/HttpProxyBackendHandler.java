package tbcloud.open.httpproxy.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.util.StringUtil;

/**
 * 请求转发到node的http服务
 *
 * @author dzh
 * @date 2018-12-17 21:11
 */
class HttpProxyBackendHandler extends AbstractInboundHandler {
    static final Logger LOG = LoggerFactory.getLogger(HttpProxyBackendHandler.class);

    private final ChannelHandlerContext inChannelContext; //  user connection

    private boolean keepAlive;


    public HttpProxyBackendHandler(ChannelHandlerContext inChannel, boolean keepAlive) {
        this.inChannelContext = inChannel;
        this.keepAlive = keepAlive;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof LastHttpContent) {
            inChannelContext.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    //
                    ctx.channel().close();
                    //
                    if (future.isSuccess()) {
                        if (!keepAlive) {
                            inChannelContext.channel().close();
                        }
                    } else {
                        Result<Void> r = new Result();
                        r.setCode(ApiCode.IO_ERROR);
                        r.setMsg("failed to write response");
                        writeError(inChannelContext, false, null, r);
                    }
                }
            });
        } else {
            if (msg instanceof HttpResponse) {
                HttpProxyRecord record = initRecord((HttpResponse) msg);
                if (record != null)
                    HttpProxyDao.updateHttpProxyRecord(record);// TODO asyn
            }

            inChannelContext.write(msg);
        }
    }

    protected HttpProxyRecord initRecord(HttpResponse msg) {
        String recordId = msg.headers().get(ApiConst.HTTPPROXY_RECORD);
        if (StringUtil.isEmpty(recordId))
            return null;

        HttpProxyRecord record = new HttpProxyRecord();
        record.setId(recordId); // primary
        record.setRspCode(((HttpResponse) msg).status().code());
        record.setRspTime(System.currentTimeMillis());
        record.setRspReason(((HttpResponse) msg).status().reasonPhrase());
        record.setRspSize(((HttpResponse) msg).headers().getInt(HttpHeaderNames.CONTENT_LENGTH));

        msg.headers().remove(ApiConst.HTTPPROXY_RECORD);
        return record;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error(cause.getLocalizedMessage(), cause);

        if (ctx.channel().isActive()) {
            if (cause instanceof ReadTimeoutException) {
                Result<Void> r = new Result<>();
                r.setCode(ApiCode.RESPONSE_TIMEOUT);
                r.setMsg(cause.getMessage());
                writeError(ctx, false, null, r);
            } else {
                Result<Void> r = new Result<>();
                r.setCode(ApiCode.ERROR_UNKNOWN);
                r.setMsg(cause.getMessage());
                writeError(ctx, false, null, r);
            }
        }
    }

//    static void closeOnFlush(Channel ch) {
//        if (ch.isActive()) {
//            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
//        }
//    }
}
