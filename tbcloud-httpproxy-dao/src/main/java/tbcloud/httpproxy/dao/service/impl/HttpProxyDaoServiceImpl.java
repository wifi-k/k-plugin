package tbcloud.httpproxy.dao.service.impl;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import jframe.mybatis.MultiMybatisService;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.httpproxy.dao.HttpProxyDaoPlugin;
import tbcloud.httpproxy.dao.service.HttpProxyDaoService;
import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.httpproxy.model.HttpProxyOnlineExample;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.httpproxy.model.HttpProxyRecordExample;
import tbcloud.httpproxy.model.mapper.HttpProxyOnlineMapper;
import tbcloud.httpproxy.model.mapper.HttpProxyRecordMapper;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2018-12-11 12:16
 */
@Injector
public class HttpProxyDaoServiceImpl implements HttpProxyDaoService {

    static Logger LOG = LoggerFactory.getLogger(HttpProxyDaoServiceImpl.class);

    @InjectPlugin
    static HttpProxyDaoPlugin Plugin;

    @InjectService(id = "jframe.service.multimybatis")
    static MultiMybatisService MultiMybatisSvc;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @Override
    public int insertHttpProxyRecord(HttpProxyRecord httpProxyRecord) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                httpProxyRecord.setCreateTime(st);
                httpProxyRecord.setUpdateTime(st);

                int r = session.getMapper(HttpProxyRecordMapper.class).insertSelective(httpProxyRecord);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(httpProxyRecord));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<HttpProxyRecord> selectHttpProxyRecord(HttpProxyRecordExample example) {
        List<HttpProxyRecord> httpProxyRecords = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            httpProxyRecords = session.getMapper(HttpProxyRecordMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return httpProxyRecords;
    }

    @Override
    public HttpProxyRecord selectHttpProxyRecord(String id) {
        HttpProxyRecord httpProxyRecord = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            httpProxyRecord = session.getMapper(HttpProxyRecordMapper.class).selectByPrimaryKey(id);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return httpProxyRecord;
    }

    @Override
    public int updateHttpProxyRecord(HttpProxyRecord httpProxyRecord) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                httpProxyRecord.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(HttpProxyRecordMapper.class).updateByPrimaryKeySelective(httpProxyRecord);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            } finally {
                //TODO rm cache redis
            }
        }
        return 0;
    }

    @Override
    public int insertHttpProxyOnline(HttpProxyOnline httpProxyOnline) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                httpProxyOnline.setCreateTime(st);
                httpProxyOnline.setUpdateTime(st);

                int r = session.getMapper(HttpProxyOnlineMapper.class).insertSelective(httpProxyOnline);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(httpProxyOnline));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public HttpProxyOnline selectHttpProxyOnline(String nodeId) {
        HttpProxyOnline httpProxyOnline = getFromRedis(ApiConst.REDIS_ID_HTTPPROXY, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ONLINE_ + nodeId, HttpProxyOnline.class);
        if (httpProxyOnline != null) return httpProxyOnline;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            httpProxyOnline = session.getMapper(HttpProxyOnlineMapper.class).selectByPrimaryKey(nodeId);
            if (httpProxyOnline != null)
                setToRedis(ApiConst.REDIS_ID_HTTPPROXY, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ONLINE_ + nodeId, GsonUtil.toJson(httpProxyOnline), ApiConst.REDIS_EXPIRED_1H);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return httpProxyOnline;
    }

    @Override
    public int updateHttpProxyOnline(HttpProxyOnline httpProxyOnline) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                httpProxyOnline.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(HttpProxyOnlineMapper.class).updateByPrimaryKeySelective(httpProxyOnline);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            } finally {
                deleteFromRedis(ApiConst.REDIS_ID_HTTPPROXY, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ONLINE_ + httpProxyOnline.getNodeId());
            }
        }
        return 0;
    }

    @Override
    public List<HttpProxyOnline> selectHttpProxyOnline(HttpProxyOnlineExample example) {
        List<HttpProxyOnline> httpProxyOnlineList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            httpProxyOnlineList = session.getMapper(HttpProxyOnlineMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return httpProxyOnlineList;
    }

    protected String getFromRedis(String id, String key) {
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                return jedis.get(key);
            }
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Type type) {
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
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
