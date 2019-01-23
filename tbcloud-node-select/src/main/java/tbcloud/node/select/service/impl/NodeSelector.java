package tbcloud.node.select.service.impl;

import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2018-12-20 20:01
 */
@Injector
public class NodeSelector {

    static Logger LOG = LoggerFactory.getLogger(NodeSelector.class);

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;


    protected String getFromRedis(String id, String key) {
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                return jedis.get(key);
            }
        }
        return null;
    }

    protected List<String> srandmemberFromRedis(String id, String key, int count) {
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                return jedis.srandmember(key, count);
            }
        }
        return Collections.emptyList();
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

}
