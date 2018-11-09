package tbcloud.user.api.http;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import redis.clients.jedis.Jedis;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.user.api.UserApiPlugin;
import tbcloud.user.api.http.req.ReqContext;

import java.lang.reflect.Type;

/**
 * @author dzh
 */
@Injector
public class BaseResource {

    @InjectPlugin
    static UserApiPlugin Plugin;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;


    public final int validateToken(ReqContext reqContext) {
        String token = reqContext.getToken();

        long usrId = IDUtil.decodeUserIDFromToken(token);
        if (usrId <= 0) return ApiCode.TOKEN_INVALID;

//        UserInfo userInfo = Dao.selectUsrInfo(usrId); TODO
//        if (userInfo == null) return ApiCode.TOKEN_INVALID;
//        reqContext.setUserInfo(userInfo); // update context

        String usrToken = null;
        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(ApiConst.REDIS_ID_USR);
            if (jedis == null) return ApiCode.REDIS_GET_NULL;

            usrToken = jedis.get(ApiConst.REDIS_KEY_TOKEN_ + usrId);
        } finally {
            if (jedis != null) Jedis.recycleJedis(ApiConst.REDIS_ID_USR, jedis);
        }

        if (usrToken == null) return ApiCode.TOKEN_EXPIRED;
        if (!usrToken.equals(token)) return ApiCode.TOKEN_INVALID;

        return ApiCode.SUCC;
    }

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

    public void setToRedis(String id, String key, Object value, Integer expiredSeconds) {
        if (value == null) return;

        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                jedis.setex(key, expiredSeconds, GsonUtil.toJson(value));
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

    // 开启调试模式 用于测试
    public boolean isDebug() {
        return "true".equalsIgnoreCase(Plugin.getConfig("debug.enabled").trim());
    }

}
