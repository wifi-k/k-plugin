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
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.dao.NodeDaoPlugin;
import tbcloud.node.dao.service.NodeDaoService;
import tbcloud.node.model.*;
import tbcloud.node.model.ext.NodeInfoRt;
import tbcloud.node.model.ext.NodeInfoRtExample;
import tbcloud.node.model.mapper.NodeAppMapper;
import tbcloud.node.model.mapper.NodeInfoMapper;
import tbcloud.node.model.mapper.NodeInsMapper;
import tbcloud.node.model.mapper.NodeRtMapper;
import tbcloud.node.model.mapper.ext.NodeInfoRtMapper;

import java.lang.reflect.Type;
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
    public NodeApp selectNodeApp(long id) {
        //TODO read cache
        NodeApp nodeApp = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeApp = session.getMapper(NodeAppMapper.class).selectByPrimaryKey(id);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeApp;
    }

    @Override
    public int insertNodeApp(NodeApp nodeApp) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                nodeApp.setCreateTime(st);
                nodeApp.setUpdateTime(st);

                int r = session.getMapper(NodeAppMapper.class).insertSelective(nodeApp);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(nodeApp));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateNodeApp(NodeApp nodeApp) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeApp.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeAppMapper.class).updateByPrimaryKeySelective(nodeApp);
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
    public int updateNodeAppSelective(NodeApp nodeApp, NodeAppExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeApp.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeAppMapper.class).updateByExampleSelective(nodeApp, example);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

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
    public void batchInsertNodeInfo(List<NodeInfo> nodeInfoList) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                for (NodeInfo nodeInfo : nodeInfoList) {
                    long st = System.currentTimeMillis();
                    nodeInfo.setCreateTime(st);
                    nodeInfo.setUpdateTime(st);

                    session.getMapper(NodeInfoMapper.class).insertSelective(nodeInfo);
                }

                LOG.info("batch insert {}", GsonUtil.toJson(nodeInfoList));
                session.commit();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
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

    @Override
    public List<NodeInfoRt> selectNodeInfoLeftJoinRt(NodeInfoRtExample example) {
        List<NodeInfoRt> nodeInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeInfoList = session.getMapper(NodeInfoRtMapper.class).selectByExampleLeftJoinRt(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeInfoList;
    }

    @Override
    public List<NodeInfoRt> selectNodeRtLeftJoinInfo(NodeRtInfoExample example) {
        List<NodeInfoRt> nodeInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeInfoList = session.getMapper(NodeInfoRtMapper.class).selectByExampleLeftJoinInfo(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeInfoList;
    }

    @Override
    public int insertNodeRt(NodeRt nodeRt) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                nodeRt.setCreateTime(st);
                nodeRt.setUpdateTime(st);

                int r = session.getMapper(NodeRtMapper.class).insertSelective(nodeRt);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(nodeRt));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<NodeRt> selectNodeRt(NodeRtExample example) {
        List<NodeRt> nodeRtList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeRtList = session.getMapper(NodeRtMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeRtList;
    }

    @Override
    public NodeRt selectNodeRt(String nodeId) {
        NodeRt nodeRt = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeRt = session.getMapper(NodeRtMapper.class).selectByPrimaryKey(nodeId);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeRt;
    }

    @Override
    public int updateNodeRt(NodeRt nodeRt) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeRt.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeRtMapper.class).updateByPrimaryKeySelective(nodeRt);
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
    public int updateNodeRtSelective(NodeRt nodeRt, NodeRtExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeRt.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeRtMapper.class).updateByExampleSelective(nodeRt, example);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public long countNodeRt(NodeRtExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                return session.getMapper(NodeRtMapper.class).countByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return -1L;
    }

    @Override
    public void batchInsertNodeRt(List<NodeRt> nodeRtList) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                for (NodeRt node : nodeRtList) {
                    long st = System.currentTimeMillis();
                    node.setCreateTime(st);
                    node.setUpdateTime(st);

                    session.getMapper(NodeRtMapper.class).insertSelective(node);
                }
                session.commit();
                LOG.info("batch insert {}", GsonUtil.toJson(nodeRtList));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
    }

    @Override
    public int insertNodeIns(NodeIns nodeIns) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                nodeIns.setCreateTime(st);
                nodeIns.setUpdateTime(st);

                int r = session.getMapper(NodeInsMapper.class).insertSelective(nodeIns);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(nodeIns));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateNodeIns(NodeIns nodeIns) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeIns.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeInsMapper.class).updateByPrimaryKeySelective(nodeIns);
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
    public List<NodeIns> selectNodeIns(NodeInsExample example) {
        List<NodeIns> nodeInsList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeInsList = session.getMapper(NodeInsMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return nodeInsList;
    }

    @Override
    public int updateNodeInsSelective(NodeIns nodeIns, NodeInsExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeIns.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeInsMapper.class).updateByExampleSelective(nodeIns, example);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
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

}
