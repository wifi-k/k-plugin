package tbcloud.open.httpproxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.AppEnum;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.open.httpproxy.handler.util.HttpProxyRecordUtil;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author dzh
 * @date 2018-12-06 20:11
 */
public class HttpProxyHandler extends AbstractInboundHandler {

    private Channel outChannel; // connect to node http server
    private String prevNodeId; //

    private static final int MAX_RETRY = 3;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            resetState();

            // don't print apikey
            long userId = IDUtil.readUserIdFromApikey(((HttpRequest) msg).headers().get(ApiConst.API_APIKEY));
            String defautNodeId = ((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_NODE); //use for test
            boolean keepAlive = HttpUtil.isKeepAlive((HttpMessage) msg);
            String policy = ((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_POLICY); //random hold
            //this.cookieString = ((HttpMessage) msg).headers().get(HttpHeaderNames.COOKIE);

            // select nodeId and connect to node proxy
            int retry = 0;
            HttpProxyOnline online = null;
            while (outChannel == null && retry++ < MAX_RETRY) {
                online = selectNode(userId, policy, defautNodeId);
                if (online == null) {
                    break;
                }
                //new record
                this.record = HttpProxyRecordUtil.toRecord(((HttpRequest) msg), HttpProxyConst.PROXY_STATUS_SUCC, online.getNodeId());
                // connect to node's http server
                this.outChannel = connectNodeServer(ctx, keepAlive, online, record);
                // TODO 连接失败的分析处理，可能的结果是从redis集合里删除这个nodeId
            }

            if (outChannel == null || !outChannel.isActive()) {
                writeResponse(ctx, false, null, newResult(ApiCode.NODE_NOT_FOUND, "failed to find proxy node"),
                        HttpProxyRecordUtil.toRecord(((HttpRequest) msg), HttpProxyConst.PROXY_STATUS_FAIL, null));
                return;
            }

            // rewrite header
            ((HttpRequest) msg).headers().add(ApiConst.HTTPPROXY_NODE, online.getNodeId());
            ((HttpRequest) msg).headers().add(ApiConst.HTTPPROXY_RECORD, record.getId());
            LOG.info("{} {} {} {} {}", userId, keepAlive, policy, online.getNodeId(), record.getId());
        }

        if (outChannel != null) {
            if (msg instanceof LastHttpContent) {
                outChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            prevNodeId = record.getNodeId();
                        } else {
                            // write error and close connection
                            Result<Void> r = newResult(ApiCode.IO_ERROR, "failed to send data");
                            record.setProxyStatus(HttpProxyConst.PROXY_STATUS_FAIL);
                            record.setRspCode(r.getCode());
                            record.setRspReason(r.getMsg());
                            writeResponse(ctx, false, null, r, record);
                            // close outChannel;
                            future.channel().close();
                        }
                    }
                });
            } else {
                outChannel.write(msg);
            }
        }
    }

    private HttpProxyOnline selectNode(long userId, String policy, String defautNodeId) {
        HttpProxyOnline online = null;
        if (!StringUtil.isEmpty(defautNodeId)) {
            // 1.优先路由到指定的节点
            online = HttpProxyDao.selectHttpProxyOnline(defautNodeId);
            if (online.getStatus() == ApiConst.IS_ONLINE) return online;
        }
        // 2.`hold`策略时，优先路由到上一次的路由器
        if (ApiConst.HTTPPROXY_POLICY_HOLD.equals(policy) && !StringUtil.isEmpty(this.prevNodeId)) {
            online = HttpProxyDao.selectHttpProxyOnline(this.prevNodeId);
            if (online.getStatus() == ApiConst.IS_ONLINE) return online;
        }
        // TODO 支持复杂的查询条件
        // 简单实现，随机选择一个可用节点
        List<String> nodeList = NodeSelector.randomOnline(AppEnum.HTTP_PROXY.getId(), 3);
        if (nodeList.size() > 0) {
            int i = ThreadLocalRandom.current().nextInt(nodeList.size());
            online = HttpProxyDao.selectHttpProxyOnline(nodeList.get(i));
        }
        return online;
    }

    private Channel connectNodeServer(ChannelHandlerContext ctx, boolean keepAlive, HttpProxyOnline online, HttpProxyRecord record) {
        final Channel inChannel = ctx.channel();

        Bootstrap b = new Bootstrap();
        b.group(inChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();

                        p.addLast(new IdleStateHandler(0, 30, 0));
                        // p.addLast(new HttpContentCompressor());

                        p.addLast(new HttpResponseDecoder(4096, 8192, 8192, true));
                        //p.addLast(new HttpContentDecompressor());

                        p.addLast(new HttpRequestEncoder());

                        p.addLast(new HttpProxyBackendHandler(ctx, keepAlive, record));
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10)
                .option(ChannelOption.SO_TIMEOUT, 30);
        //.option(ChannelOption.AUTO_READ, true);
        ChannelFuture f = b.connect(online.getServerIp(), online.getServerPort());
        try {
            f.sync();
            return f.channel();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            f.channel().close();
        }
        return null;
    }

    protected void resetState() {
        if (outChannel != null && outChannel.isActive()) outChannel.close();
        outChannel = null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error(cause.getMessage(), cause);

        Result<Void> r = new Result<>();
        r.setCode(ApiCode.ERROR_UNKNOWN);
        r.setMsg(cause.getMessage());
        if (record != null) {
            record.setProxyStatus(HttpProxyConst.PROXY_STATUS_FAIL);
        }

        writeResponse(ctx, false, null, r, record);

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
            writeResponse(ctx, false, null, r, record);
        }
    }
}
