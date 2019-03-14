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
import tbcloud.node.model.mapper.*;
import tbcloud.node.model.mapper.ext.NodeInfoRtMapper;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    public NodeWifi selectNodeWifi(long id) {
        NodeWifi nodeWifi = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeWifi = session.getMapper(NodeWifiMapper.class).selectByPrimaryKey(id);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeWifi;
    }

    @Override
    public int insertNodeWifi(NodeWifi nodeWifi) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                nodeWifi.setCreateTime(st);
                nodeWifi.setUpdateTime(st);

                int r = session.getMapper(NodeWifiMapper.class).insertSelective(nodeWifi);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(nodeWifi));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<NodeWifi> selectNodeWifi(NodeWifiExample example) {
        List<NodeWifi> nodeWifiList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeWifiList = session.getMapper(NodeWifiMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeWifiList;
    }

    @Override
    public int updateNodeWifi(NodeWifi nodeWifi) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeWifi.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeWifiMapper.class).updateByPrimaryKeySelective(nodeWifi);
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
    public int updateNodeWifiSelective(NodeWifi nodeWifi, NodeWifiExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                nodeWifi.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(NodeWifiMapper.class).updateByExampleSelective(nodeWifi, example);
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
    public NodeApp selectNodeApp(long id) {
        NodeApp nodeApp = getFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_APP_ + id, NodeApp.class);
        if (nodeApp != null) return nodeApp;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeApp = session.getMapper(NodeAppMapper.class).selectByPrimaryKey(id);
            if (nodeApp != null) {
                setToRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_APP_ + id, GsonUtil.toJson(nodeApp), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
    public List<NodeApp> selectNodeApp(NodeAppExample example) {
        List<NodeApp> nodeAppList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeAppList = session.getMapper(NodeAppMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeAppList;
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
                deleteFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_APP_ + nodeApp.getId());
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
    public boolean batchInsertNodeInfo(Set<NodeInfo> nodeInfoList) {
        if (nodeInfoList == null || nodeInfoList.isEmpty()) return false;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                for (NodeInfo nodeInfo : nodeInfoList) {
                    long st = System.currentTimeMillis();
                    nodeInfo.setCreateTime(st);
                    nodeInfo.setUpdateTime(st);

                    session.getMapper(NodeInfoMapper.class).insertSelective(nodeInfo);
                }

                LOG.info("batch insert {} {}", nodeInfoList.size(), GsonUtil.toJson(nodeInfoList));
                session.commit();
                return true;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return false;
    }

    @Override
    public List<NodeInfo> selectNodeInfo(NodeInfoExample example) {
        List<NodeInfo> nodeInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeInfoList = session.getMapper(NodeInfoMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeInfoList;
    }

    @Override
    public NodeInfo selectNodeInfo(String nodeId) {
        NodeInfo nodeInfo = getFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_INFO_ + nodeId, NodeInfo.class);
        if (nodeInfo != null) return nodeInfo;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeInfo = session.getMapper(NodeInfoMapper.class).selectByPrimaryKey(nodeId);
            if (nodeInfo != null) {
                setToRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_INFO_ + nodeId, GsonUtil.toJson(nodeInfo), ApiConst.REDIS_EXPIRED_24H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
                deleteFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_INFO_ + nodeInfo.getNodeId());
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
            nodeInfoList = session.getMapper(NodeInfoRtMapper.class).selectByExampleLeftJoinRt(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeInfoList;
    }

    @Override
    public List<NodeInfoRt> selectNodeRtLeftJoinInfo(NodeRtInfoExample example) {
        List<NodeInfoRt> nodeInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeInfoList = session.getMapper(NodeInfoRtMapper.class).selectByExampleLeftJoinInfo(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
            nodeRtList = session.getMapper(NodeRtMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeRtList;
    }

    @Override
    public NodeRt selectNodeRt(String nodeId) {
        NodeRt nodeRt = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeRt = session.getMapper(NodeRtMapper.class).selectByPrimaryKey(nodeId);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
    public boolean batchInsertNodeRt(Set<NodeRt> nodeRtList) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                for (NodeRt node : nodeRtList) {
                    long st = System.currentTimeMillis();
                    node.setCreateTime(st);
                    node.setUpdateTime(st);

                    session.getMapper(NodeRtMapper.class).insertSelective(node);
                }
                session.commit();
                LOG.info("batch insert {} {}", nodeRtList.size(), GsonUtil.toJson(nodeRtList));
                return true;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return false;
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
            }
        }
        return 0;
    }

    @Override
    public boolean batchUpdateNodeIns(List<NodeIns> nodeInsList) {
        if (nodeInsList == null || nodeInsList.isEmpty()) return false;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                for (NodeIns nodeIns : nodeInsList) {
                    nodeIns.setUpdateTime(st);

                    session.getMapper(NodeInsMapper.class).updateByPrimaryKeySelective(nodeIns);
                }

                LOG.info("batch update {} {}", nodeInsList.size(), GsonUtil.toJson(nodeInsList));
                session.commit();
                return true;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return false;
    }

    @Override
    public List<NodeIns> selectNodeIns(NodeInsExample example) {
        List<NodeIns> nodeInsList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            nodeInsList = session.getMapper(NodeInsMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
