package tbcloud.node.httpproxy.tcp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import jframe.core.msg.PluginMsg;
import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.httpproxy.dao.service.HttpProxyDaoService;
import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.httpproxy.protocol.ByteBufHttpProxy;
import tbcloud.httpproxy.protocol.HttpProxyDataType;
import tbcloud.httpproxy.protocol.codec.HttpProxyDataCodecFactory;
import tbcloud.httpproxy.protocol.data.*;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.httpproxy.NodeHttpProxyPlugin;
import tbcloud.node.protocol.DataType;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.codec.DataCodec;
import tbcloud.node.protocol.codec.DataCodecFactory;

/**
 * @author dzh
 * @date 2018-12-14 10:53
 */
@Injector
public class HttpProxyDataHandler extends SimpleChannelInboundHandler<ByteBufHttpProxy> {
    static Logger LOG = LoggerFactory.getLogger(HttpProxyDataHandler.class);

    static final DataCodecFactory DataCodecFactory = new HttpProxyDataCodecFactory();

    @InjectPlugin
    static NodeHttpProxyPlugin Plugin;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @InjectService(id = "tbcloud.service.httpproxy.dao")
    static HttpProxyDaoService HttpProxyDao;

    private String nodeId; //TODO how to tag channel with nodeId

    private boolean closed = false;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBufHttpProxy msg) throws Exception {
        DataAck ack = null;
        switch (msg.dataType()) {
            case HttpProxyDataType.HPROXY_JOIN:
                ack = doJoin(msg);
                if (isSucc(ack)) {
                    this.nodeId = nodeId;
                    Plugin.dispatch().attachNode(nodeId, ctx);
                }
                break;
            case HttpProxyDataType.HPROXY_HEARTBEAT:
                ack = doHeartbeat(msg);
                break;
            case HttpProxyDataType.HPROXY_RESPONSE:
                ack = doProxyResponse(msg);
                break;
            case HttpProxyDataType.HPROXY_QUIT:
                ack = doQuit(msg);
                if (isSucc(ack)) this.closed = true; //close channel
                break;
            default:
                LOG.error("discard error dataType {} {}", msg.dataType(), msg.id());
        }

        ChannelFuture f = writeAck(ctx, msg, ack);
        if (closed) f.addListener(ChannelFutureListener.CLOSE);
    }

    private ChannelFuture writeAck(ChannelHandlerContext ctx, ByteBufHttpProxy req, DataAck ack) {
        if (ack == null) {
            ack = new DataAck();
            ack.setCode(ApiCode.ERROR_UNKNOWN);
        }

        ByteBufHttpProxy msg = new ByteBufHttpProxy(); //response
        msg.magic(PacketConst.M);
        msg.version(PacketConst.V_20181130);
        msg.id(req.id());
        msg.token("");
        msg.dataType(DataType.I.rspType(req.dataType()));
        msg.dataFormat((byte) PacketConst.DataFormat.JSON.ordinal());
        msg.data(DataCodecFactory.codec(msg.dataType(), msg.dataFormat()).encode(ack));
        msg.hash(0L);
        return ctx.writeAndFlush(msg);
    }

    /**
     * 更新离开信息
     *
     * @param msg
     * @return {@link DataAck}
     */
    private DataAck doQuit(ByteBufHttpProxy msg) {
        QuitHttpProxy data = decodeData(msg, QuitHttpProxy.class);
        String nodeId = data.getNodeId();
        if (!isValidToken(msg.token(), nodeId)) {
            DataAck ack = new DataAck();
            ack.setCode(ApiCode.TOKEN_INVALID);
            return ack;
        }

        updateOffline(nodeId);

        DataAck ack = new DataAck();
        ack.setCode(ApiCode.SUCC);

        return ack;
    }

    private void updateOffline(String nodeId) {
        HttpProxyOnline offline = new HttpProxyOnline();
        offline.setNodeId(nodeId);
        offline.setOfflineTime(System.currentTimeMillis());
        offline.setStatus(ApiConst.IS_OFFLINE);


        Plugin.sendToNode(new PluginMsg<String>().setType(MsgType.NODE_QUIT_HTTPPROXY).setValue(GsonUtil.toJson(offline)), nodeId);
//        Plugin.send(new PluginMsg<HttpProxyOnline>().setType(MsgType.NODE_QUIT_HTTPPROXY).setValue(offline));
        // HttpProxyDao.updateHttpProxyOnline(offline);
    }

    private DataAck doProxyResponse(ByteBufHttpProxy msg) {
        HttpProxyResponse data = decodeData(msg, HttpProxyResponse.class);
        String recordId = data.getId();
        String nodeId = data.getNodeId();
        if (!isValidToken(msg.token(), nodeId)) {
            DataAck ack = new DataAck();
            ack.setCode(ApiCode.TOKEN_INVALID);
            return ack;
        }

        //TODO ack code
        Plugin.dispatch().findNode(nodeId).writeResponse(data);

        DataAck ack = new DataAck();
        ack.setCode(ApiCode.SUCC);

        return ack;
    }

    /**
     * @param msg
     * @return {@link DataAck}
     */
    private DataAck doHeartbeat(ByteBufHttpProxy msg) {
        Heartbeat data = decodeData(msg, Heartbeat.class);
        String nodeId = data.getNodeId();
        if (!isValidToken(msg.token(), nodeId)) {
            DataAck ack = new DataAck();
            ack.setCode(ApiCode.TOKEN_INVALID);
            return ack;
        }

        DataAck ack = new DataAck();
        ack.setCode(ApiCode.SUCC);

        return ack;
    }

    /**
     * 更新在线信息、对应的http服务信息
     *
     * @param msg
     * @return {@link DataAck}
     */
    private DataAck doJoin(ByteBufHttpProxy msg) {
        JoinHttpProxy data = decodeData(msg, JoinHttpProxy.class);
        String nodeId = data.getNodeId();
        if (!isValidToken(msg.token(), nodeId)) {
            DataAck ack = new DataAck();
            ack.setCode(ApiCode.TOKEN_INVALID);
            return ack;
        }

        // update online
        HttpProxyOnline online = new HttpProxyOnline();
        online.setNodeId(nodeId);
        online.setOnlineTime(System.currentTimeMillis());
        online.setToken(msg.token());
        online.setStatus(ApiConst.IS_ONLINE);
        // server info
        online.setServerId(Plugin.serverId());
        online.setServerIp(Plugin.getHttpServer().getIp());
        online.setServerPort(Plugin.getHttpServer().getPort());

        // httpproxy_online
//        HttpProxyOnline oldOnline = HttpProxyDao.selectHttpProxyOnline(nodeId);
//        if (oldOnline == null) {
//            HttpProxyDao.insertHttpProxyOnline(online);
//        } else {
//            HttpProxyDao.updateHttpProxyOnline(online);
//        }
        // async
        Plugin.sendToNode(new PluginMsg<String>().setType(MsgType.NODE_JOIN_HTTPPROXY).setValue(GsonUtil.toJson(online)), nodeId);
//        Plugin.send(new PluginMsg<HttpProxyOnline>().setType(MsgType.NODE_JOIN_HTTPPROXY).setValue(online));

        DataAck ack = new DataAck();
        ack.setCode(ApiCode.SUCC);
        return ack;
    }

    private boolean isSucc(DataAck ack) {
        return ack.getCode() == ApiCode.SUCC;
    }

    private <T> T decodeData(ByteBufHttpProxy msg, Class data) {
        DataCodec dataCodec = DataCodecFactory.codec(msg.dataType(), msg.dataFormat());
        return (T) dataCodec.decode(msg.data(), data);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error(cause.getMessage(), cause);
        if (cause instanceof ReadTimeoutException) {
            // update offline info
            updateOffline(this.nodeId);
        }
        // close
        ctx.channel().close();
        //
        clearDispatchNode();
    }

    private void clearDispatchNode() {
        Plugin.dispatch().detachNode(this.nodeId);
    }

//    private void writeAndClose(ChannelHandlerContext ctx, Object msg) {
//        ctx.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
//    }

    private boolean isValidToken(String token, String nodeId) {
        if (StringUtil.isEmpty(nodeId) || StringUtil.isEmpty(token))
            return false;

        if (IDUtil.isInvalidToken(token, nodeId)) {
            LOG.info("isInvalidToken {} {}", token, nodeId);
            return false;
        }

        String authNodeId = getFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_TOKEN_ + token);
        if (StringUtil.isEmpty(authNodeId) || !authNodeId.equals(nodeId)) {
            LOG.info("isInvalidToken {} {} {}", token, nodeId, authNodeId);
            return false;
        }
        return true;
    }

    protected String getFromRedis(String id, String key) {
        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                return jedis.get(key);
            }
        }
        return null;
    }

}
