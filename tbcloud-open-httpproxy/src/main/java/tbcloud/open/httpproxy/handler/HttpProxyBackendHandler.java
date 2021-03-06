package tbcloud.open.httpproxy.handler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import jframe.core.msg.TextMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;

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

    public HttpProxyBackendHandler(ChannelHandlerContext inChannel, boolean keepAlive, HttpProxyRecord record) {
        super(false);

        this.inChannelContext = inChannel;
        this.keepAlive = keepAlive;
        this.record = record;

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        LOG.info("HttpProxyBackendHandler msg start refCnt {}", ReferenceCountUtil.refCnt(msg));
        if (msg instanceof LastHttpContent) {
            // FIXED io.netty.util.IllegalReferenceCountException: refCnt: 0, decrement: 1
            inChannelContext.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    LOG.info("operationComplete {} {}", future.isSuccess(), future.cause());
                    if (future.isSuccess()) {
                        if (!keepAlive) {
                            inChannelContext.channel().close();
                        }
                        // send
                        Plugin.sendToHttpProxy(new TextMsg().setType(MsgType.HTTPPROXY_RECORD_ADD).setValue(GsonUtil.toJson(record)));
                    } else {
                        Result<Void> r = new Result();
                        r.setCode(ApiCode.IO_ERROR);
                        r.setMsg("failed to write response");
                        writeResponse(inChannelContext, false, null, r, record, msg);
                    }
                    //
                    ctx.channel().close();
                }
            });
        } else {
            if (msg instanceof HttpResponse) {
                updateRecordRsp((HttpResponse) msg);
            }

            inChannelContext.write(msg);
        }
        LOG.info("HttpProxyBackendHandler msg end refCnt {}", ReferenceCountUtil.refCnt(msg));
    }

    protected HttpProxyRecord updateRecordRsp(HttpResponse msg) {
        long st = System.currentTimeMillis();
        record.setRspCode(((HttpResponse) msg).status().code());
        record.setRspTime(st);
        record.setRspReason(((HttpResponse) msg).status().reasonPhrase());
        record.setRspSize(((HttpResponse) msg).headers().getInt(HttpHeaderNames.CONTENT_LENGTH));
        record.setProxyCost((int) (st - record.getReqTime().longValue()));

        // error result
        int errCode = ((HttpResponse) msg).headers().getInt(ApiConst.HTTPPROXY_ERROR_CODE, -1);
        if (errCode > 0) {
            record.setProxyStatus(errCode == ApiCode.RESPONSE_TIMEOUT ? HttpProxyConst.PROXY_STATUS_TIMEOUT : HttpProxyConst.PROXY_STATUS_FAIL);
            record.setRspCode(errCode);
            record.setRspReason(((HttpResponse) msg).headers().get(ApiConst.HTTPPROXY_ERROR_MSG));
        } else {
            record.setProxyStatus(HttpProxyConst.PROXY_STATUS_SUCC);
        }

        LOG.info("updateRecordRsp {} {}", record.getId(), record.getProxyStatus());
        return record;
    }

//    static void closeOnFlush(Channel ch) {
//        if (ch.isActive()) {
//            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
//        }
//    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("HttpProxyBackendHandler " + cause.getMessage(), cause);

        Result<Void> r = new Result<>();
        r.setCode(ApiCode.ERROR_UNKNOWN);
        r.setMsg(cause.getMessage());
        if (record != null) {
            record.setProxyStatus(HttpProxyConst.PROXY_STATUS_FAIL);
        }
        writeResponse(inChannelContext, false, null, r, record, null);
        //
        ctx.channel().close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) { //timeout
            Result<Void> r = new Result<>();
            r.setCode(ApiCode.REQUEST_TIMEOUT);
            r.setMsg("timeout");

            if (record != null) {
                record.setProxyStatus(HttpProxyConst.PROXY_STATUS_TIMEOUT);
            }
            writeResponse(inChannelContext, false, null, r, record, null);
        }
        ctx.channel().close();
    }
}
