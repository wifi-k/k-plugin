package tbcloud.open.httpproxy.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.AppEnum;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author dzh
 * @date 2018-12-06 20:11
 */
public class HttpProxyHandler extends AbstractInboundHandler {

    private String nodeId; // 当前请求到的NodeId;keepAlive时，希望保持请求到同一个节点
    private Channel outChannel; // connect to node http server

    private static final int MAX_RETRY = 3;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            if (outChannel != null) { // keepAlive
                if (outChannel.isActive()) outChannel.close();
                outChannel = null;
            }

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
                // connect to node's http server
                outChannel = connectNodeServer(ctx, keepAlive, online);
            }

            if (outChannel == null || !outChannel.isActive()) {
                writeError(ctx, false, null, newResult(ApiCode.NODE_NOT_FOUND, "failed to find proxy connection"));
                return;
            }

            this.nodeId = online.getNodeId();
            //new record
            HttpProxyRecord record = initRecord((HttpRequest) msg, online);
            HttpProxyDao.insertHttpProxyRecord(record); // TODO async

            // rewrite header
            ((HttpRequest) msg).headers().add(ApiConst.HTTPPROXY_NODE, online.getNodeId());
            ((HttpRequest) msg).headers().add(ApiConst.HTTPPROXY_RECORD, record.getId());
            // ((HttpRequest) msg).headers().add(ApiConst.HTTPPROXY_REQTIME, String.valueOf(record.getReqTime()));
            LOG.info("{} {} {} {} {}", userId, keepAlive, policy, online.getNodeId(), record.getId());
        }

        if (msg instanceof LastHttpContent) {
            outChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // do nothing
                    } else {
                        future.channel().close(); // close outChannel;
                        // write error and close connection
                        writeError(ctx, false, null, newResult(ApiCode.IO_ERROR, "failed to send data"));
                    }
                }
            });
        } else {
            outChannel.write(msg);
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
        if (ApiConst.HTTPPROXY_POLICY_HOLD.equals(policy) && !StringUtil.isEmpty(this.nodeId)) { // TODO redis
            online = HttpProxyDao.selectHttpProxyOnline(this.nodeId);
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

    private Channel connectNodeServer(ChannelHandlerContext ctx, boolean keepAlive, HttpProxyOnline online) {
        final Channel inChannel = ctx.channel();

        Bootstrap b = new Bootstrap();
        b.group(inChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();

                        p.addLast(new ReadTimeoutHandler(30));
                        p.addLast(new HttpRequestEncoder());
                        p.addLast(new HttpContentCompressor());
                        p.addLast(new HttpResponseDecoder(4096, 8192, 8192, true));
                        p.addLast(new HttpContentDecompressor());

                        p.addLast(new HttpProxyBackendHandler(ctx, keepAlive));
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30)
                .option(ChannelOption.SO_TIMEOUT, 30);//TODO config
        //.option(ChannelOption.AUTO_READ, false);
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


    private HttpProxyRecord initRecord(HttpRequest msg, HttpProxyOnline online) {
        HttpProxyRecord record = new HttpProxyRecord();
        record.setId(IDUtil.genHttpProxyId(Plugin.serverId()));

        long userId = IDUtil.readUserIdFromApikey(msg.headers().get(ApiConst.API_APIKEY));
        record.setUserId(userId);

        record.setNodeId(online.getNodeId());
        record.setReqProtocol(msg.protocolVersion().protocolName());
        record.setReqMethod(msg.method().name());
        record.setReqSize(msg.headers().getInt(HttpHeaderNames.CONTENT_LENGTH));
        record.setReqTime(System.currentTimeMillis());
        record.setReqUri(msg.uri());

        return record;
    }

    protected void resetState() {
        if (outChannel != null && outChannel.isActive()) outChannel.close();
        outChannel = null;
    }
}
