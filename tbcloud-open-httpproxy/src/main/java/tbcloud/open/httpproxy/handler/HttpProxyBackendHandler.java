package tbcloud.open.httpproxy.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
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
                    if (!keepAlive) {
                        inChannelContext.channel().close();
                    }
                }
            });
        } else {
            if (msg instanceof HttpResponse) {
                HttpProxyRecord record = initRecord((HttpResponse) msg);
                if (record != null)
                    HttpProxyDao.updateHttpProxyRecord(record);// TODO asyn
            }

            inChannelContext.write(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        //
                        ctx.channel().close();
                        //
                        Result<Void> r = new Result();
                        r.setCode(ApiCode.IO_ERROR);
                        r.setMsg("failed to write response");
                        writeError(inChannelContext, false, null, r);
                    }
                }
            });
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
        return record;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error(cause.getLocalizedMessage(), cause);

        ctx.close();
    }

//    static void closeOnFlush(Channel ch) {
//        if (ch.isActive()) {
//            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
//        }
//    }
}
