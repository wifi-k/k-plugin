package tbcloud.node.dao.service.impl;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import jframe.mybatis.MultiMybatisService;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.dao.NodeDaoPlugin;
import tbcloud.node.dao.service.NodeDaoService;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeInfoExample;
import tbcloud.node.model.mapper.NodeInfoMapper;

import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2018-11-12 17:11
 */
@Injector
public class NodeDaoServiceImpl implements NodeDaoService {

    static Logger LOG = LoggerFactory.getLogger(NodeDaoServiceImpl.class);

    @InjectPlugin
    static NodeDaoPlugin Plugin;

    @InjectService(id = "jframe.service.multimybatis")
    static MultiMybatisService MultiMybatisSvc;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @Override
    public int insertNodeInfo(NodeInfo nodeInfo) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                nodeInfo.setCreateTime(st);
                nodeInfo.setUpdateTime(st);

                int r = session.getMapper(NodeInfoMapper.class).insertSelective(nodeInfo);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(nodeInfo));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<NodeInfo> selectNodeInfo(NodeInfoExample example) {
        List<NodeInfo> nodeInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeInfoList = session.getMapper(NodeInfoMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeInfoList;
    }

    @Override
    public NodeInfo selectNodeInfo(String nodeId) {
        //TODO read cache
        NodeInfo nodeInfo = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeInfo = session.getMapper(NodeInfoMapper.class).selectByPrimaryKey(nodeId);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeInfo;
    }

    @Override
    public int updateNodeInfo(NodeInfo nodeInfo) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeInfo.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeInfoMapper.class).updateByPrimaryKeySelective(nodeInfo);
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
    public long countNodeInfo(NodeInfoExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                return session.getMapper(NodeInfoMapper.class).countByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return -1L;
    }

    private String getFromRedis(String id, String key) {
        redis.clients.jedis.Jedis jedis = null;
        try {
            jedis = Jedis.getJedis(id);
            if (jedis != null) {
                return jedis.get(key);
            }
        } finally {
            if (jedis != null) Jedis.recycleJedis(id, jedis);
        }
        return "";
    }

    private void setToRedis(String id, String key, String value, int expiredSeconds) {
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

    private void deleteFromRedis(String id, String key) {
        redis.clients.jedis.Jedis jedis = null;
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
