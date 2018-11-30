package tbcloud.node.api.handler;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.api.NodeApiPlugin;
import tbcloud.node.dao.service.NodeDaoService;
import tbcloud.node.protocol.ByteBufNodePacket;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.data.DataReq;
import tbcloud.node.protocol.data.DataRsp;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author dzh
 * @date 2018-11-28 15:52
 */
@Injector
public abstract class DataHandler<T extends DataReq> implements Runnable {

    protected static final Logger LOG = LoggerFactory.getLogger(DataHandler.class);

    @InjectPlugin
    static NodeApiPlugin Plugin;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @InjectService(id = "tbcloud.service.node.dao")
    static NodeDaoService NodeDao;

    private IoContext context;

    public DataHandler(IoContext context) {
        this.context = context;
    }

    public IoContext context() {
        return context;
    }

    public NodeApiPlugin plugin() {
        return Plugin;
    }

    protected void catchException(Exception e, T dataReq) {
        LOG.error(e.getMessage(), e);

        ByteBufNodePacket packet = context.request();
        LOG.error("handle {} {} {} {}", context.remote(), packet.id(), packet.token(), dataReq);
    }

    @Override
    public void run() {
        T dataReq = decodeDataReq(context);
        LOG.info("run {} {}", context.request().id(), dataReq);
        if (isValidToken(context, dataReq)) {
            DataRsp<?> rsp = handle(dataReq);
            try {
                context.write(rsp);
            } catch (IOException e) {
                catchException(e, dataReq);
                LOG.error("write error! {}", rsp);
            }
        } else {
            try { // invalid token, maybe expired
                context.write(ApiCode.TOKEN_INVALID);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    protected abstract DataRsp<?> handle(T dataReq);

    /**
     * 解析packet里的data
     *
     * @param context
     * @return
     */
    protected abstract T decodeDataReq(IoContext context);

    private boolean isValidToken(final IoContext context, T dataReq) {
        ByteBufNodePacket req = context.request();
        int dataType = req.dataType();
        if (dataType == PacketConst.AUTH) { // 认证节点没有Token
            return true;
        }

        String token = req.token();
        String nodeId = dataReq.getNodeId();
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


    // TODO wrapper
    protected String getFromRedis(String id, String key) {
        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                return jedis.get(key);
            }
        } finally {
            if (jedis != null) Jedis.recycleJedis(id, jedis);
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Type type) {
        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                String json = jedis.get(key);
                if (!StringUtil.isEmpty(json)) {
                    return GsonUtil.<T>fromJson(json, type);
                }
            }
        } finally {
            if (jedis != null) Jedis.recycleJedis(id, jedis);
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                String json = jedis.get(key);
                if (!StringUtil.isEmpty(json)) {
                    return GsonUtil.<T>fromJson(json, clazz);
                }
            }
        } finally {
            if (jedis != null) Jedis.recycleJedis(id, jedis);
        }
        return null;
    }

    public void setToRedis(String id, String key, String value, Integer expiredSeconds) {
        if (value == null) return;

        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                jedis.setex(key, expiredSeconds, value);
            }
        } finally {
            if (jedis != null) Jedis.recycleJedis(id, jedis);
        }
    }

    public void deleteFromRedis(String id, String key) {
        if (StringUtil.isEmpty(key)) return;

        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                jedis.del(key);
            }
        } finally {
            if (jedis != null) Jedis.recycleJedis(id, jedis);
        }
    }


}
