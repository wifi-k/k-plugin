package tbcloud.user.api.http;

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
import tbcloud.node.dao.service.NodeDaoService;
import tbcloud.user.api.UserApiPlugin;
import tbcloud.user.api.http.req.ReqContext;
import tbcloud.user.dao.service.UserDaoService;
import tbcloud.user.model.UserImgCode;
import tbcloud.user.model.UserInfo;

import java.lang.reflect.Type;

/**
 * @author dzh
 */
@Injector
public class BaseResource {

    static Logger LOG = LoggerFactory.getLogger(BaseResource.class);

    @InjectPlugin
    static UserApiPlugin Plugin;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @InjectService(id = "tbcloud.service.user.dao")
    static UserDaoService UserDao;

    @InjectService(id = "tbcloud.service.node.dao")
    static NodeDaoService NodeDao;

    public final int validateToken(ReqContext reqContext) {
        String token = reqContext.getToken();

        long usrId = IDUtil.decodeUserIDFromToken(token);
        if (usrId <= 0) return ApiCode.TOKEN_INVALID;

        UserInfo userInfo = UserDao.selectUserInfo(usrId);
        if (userInfo == null) return ApiCode.TOKEN_INVALID;
        reqContext.setUserInfo(userInfo); // update context

        String usrToken = null;
        Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(ApiConst.REDIS_ID_USER);
            if (jedis == null) return ApiCode.REDIS_GET_NULL;

            usrToken = jedis.get(ApiConst.REDIS_KEY_USER_TOKEN_ + usrId);
        } finally {
            if (jedis != null) Jedis.recycleJedis(ApiConst.REDIS_ID_USER, jedis);
        }

        if (usrToken == null) return ApiCode.TOKEN_EXPIRED;
        if (!usrToken.equals(token)) return ApiCode.TOKEN_INVALID;

        return ApiCode.SUCC;
    }

    protected String getFromRedis(String id, String key) {
        try (Jedis jedis = Jedis.getJedis(id)) {
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

    public boolean isValidImgCode(String imgCodeId, String imgCode) {
        UserImgCode imgCodeReal = UserDao.selectUserImgCode(IDUtil.innerImgCodeId(imgCodeId));
        if (imgCodeReal == null || !imgCodeReal.getImgCode().equalsIgnoreCase(imgCode)) {
            LOG.warn("invalid imgCode {} {}", imgCode, imgCodeReal.getImgCode());
            return false;
        }
        return true;
    }

    public boolean isValidVcode(String mobile, String vcode) {
        String vcodeRedis = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile);
        if (vcodeRedis == null || !vcodeRedis.equalsIgnoreCase(vcode)) {
            LOG.warn("invalid vcode {} {}", vcode, vcodeRedis);
            return false;
        }
        return true;
    }
}
