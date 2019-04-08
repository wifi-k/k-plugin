package tbcloud.user.dao.service.impl;

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
import tbcloud.user.dao.UserDaoPlugin;
import tbcloud.user.dao.service.UserDaoService;
import tbcloud.user.model.*;
import tbcloud.user.model.mapper.*;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2018-11-09 18:35
 */
@Injector
public class UserDaoServiceImpl implements UserDaoService {

    static Logger LOG = LoggerFactory.getLogger(UserDaoServiceImpl.class);

    @InjectPlugin
    static UserDaoPlugin Plugin;

    @InjectService(id = "jframe.service.multimybatis")
    static MultiMybatisService MultiMybatisSvc;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @Override
    public UserImgCode selectUserImgCode(long id) {
        String json = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_IMGCODE_ + id);
        if (!StringUtil.isEmpty(json)) {
            return GsonUtil.fromJson(json, UserImgCode.class);
        }

        UserImgCode imgCode = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            imgCode = session.getMapper(UserImgCodeMapper.class).selectByPrimaryKey(id);
            if (imgCode != null) {  //TODO async
                setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_IMGCODE_ + id, GsonUtil.toJson(imgCode), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return imgCode;
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


    @Override
    public List<UserNode> selectUserNode(UserNodeExample example) {
        List<UserNode> list = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            list = session.getMapper(UserNodeMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return list;
    }

    @Override
    public UserNode selectUserNode(long id) {
        UserNode userNode = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            userNode = session.getMapper(UserNodeMapper.class).selectByPrimaryKey(id);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return userNode;
    }

    @Override
    public int insertUserNode(UserNode userNode) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userNode.setCreateTime(st);
                userNode.setUpdateTime(st);

                int r = session.getMapper(UserNodeMapper.class).insertSelective(userNode);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userNode));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateUserNode(UserNode userNode) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userNode.setCreateTime(st);
                userNode.setUpdateTime(st);

                int r = session.getMapper(UserNodeMapper.class).insertSelective(userNode);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userNode));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public void batchUpdateUserNode(List<UserNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                for (UserNode node : nodes) {
                    node.setUpdateTime(st);
                    session.getMapper(UserNodeMapper.class).updateByPrimaryKeySelective(node);
                }
                session.commit();

//                nodeList.forEach(node -> { //TODO pipeline
//                    deleteFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_INFO_ + node.getNodeId());
//                });
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
    }

    @Override
    public int updateUserNodeSelective(UserNode userNode, UserNodeExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                userNode.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserNodeMapper.class).updateByExampleSelective(userNode, example);
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
    public long countUserNode(UserNodeExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(UserNodeMapper.class).countByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<UserMessage> selectUserMessage(UserMessageExample example) {
        List<UserMessage> messageList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            messageList = session.getMapper(UserMessageMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return messageList;
    }

    @Override
    public UserMessage selectUserMessage(long id) {
        UserMessage message = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            message = session.getMapper(UserMessageMapper.class).selectByPrimaryKey(id);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return message;
    }

    @Override
    public int batchInsertUserMessage(List<UserMessage> messages) {
        if (messages == null || messages.isEmpty()) return 0;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                for (UserMessage message : messages) {
                    long st = System.currentTimeMillis();
                    message.setCreateTime(st);
                    message.setUpdateTime(st);

                    session.getMapper(UserMessageMapper.class).insertSelective(message);
                }

                session.commit();
                LOG.info("batchInsertUserMessage {}", messages.size());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return messages.size();
    }

    @Override
    public int insertUserMessage(UserMessage message) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                message.setCreateTime(st);
                message.setUpdateTime(st);

                int r = session.getMapper(UserMessageMapper.class).insertSelective(message);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(message));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateUserMessage(UserMessage message) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                message.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserMessageMapper.class).updateByPrimaryKeySelective(message);
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
    public List<UserInfo> selectUserInfo(UserInfoExample example) {
        List<UserInfo> userInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            userInfoList = session.getMapper(UserInfoMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return userInfoList;
    }

    @Override
    public UserInfo selectUserInfo(long userId) {
        String json = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_INFO_ + userId);
        if (!StringUtil.isEmpty(json)) {
            return GsonUtil.fromJson(json, UserInfo.class);
        }

        UserInfo userInfo = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            userInfo = session.getMapper(UserInfoMapper.class).selectByPrimaryKey(userId);
            if (userInfo != null) {
                setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_INFO_ + userId, GsonUtil.toJson(userInfo), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return userInfo;
    }

    @Override
    public int insertUserInfo(UserInfo userInfo) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userInfo.setCreateTime(st);
                userInfo.setUpdateTime(st);

                int r = session.getMapper(UserInfoMapper.class).insertSelective(userInfo);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userInfo));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateUserInfo(UserInfo userInfo) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                userInfo.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserInfoMapper.class).updateByPrimaryKeySelective(userInfo);
                session.commit();
                if (r > 0) {
                    deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_INFO_ + userInfo.getId());
                }
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public UserOnline selectUserOnline(long userId) {
        UserOnline userOnline = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            userOnline = session.getMapper(UserOnlineMapper.class).selectByPrimaryKey(userId);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return userOnline;
    }

    @Override
    public int insertUserOnline(UserOnline userOnline) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userOnline.setCreateTime(st);
                userOnline.setUpdateTime(st);

                int r = session.getMapper(UserOnlineMapper.class).insertSelective(userOnline);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userOnline));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateUserOnline(UserOnline userOnline) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                userOnline.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserOnlineMapper.class).updateByPrimaryKeySelective(userOnline);
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
    public List<UserOnline> selectUserOnline(UserOnlineExample example) {
        List<UserOnline> userOnlineList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            userOnlineList = session.getMapper(UserOnlineMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return userOnlineList;
    }

    @Override
    public int updateUserOnlineSelective(UserOnline userOnline, UserOnlineExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                userOnline.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserOnlineMapper.class).updateByExampleSelective(userOnline, example);
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
    public List<UserShareRecord> selectUserShareRecord(UserShareRecordExample example) {
        List<UserShareRecord> shareRecordList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            shareRecordList = session.getMapper(UserShareRecordMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return shareRecordList;
    }

    @Override
    public int insertUserShareRecord(UserShareRecord userShareRecord) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userShareRecord.setCreateTime(st);
                userShareRecord.setUpdateTime(st);

                int r = session.getMapper(UserShareRecordMapper.class).insertSelective(userShareRecord);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userShareRecord));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<UserShareDay> selectUserShareDay(UserShareDayExample example) {
        List<UserShareDay> shareDayList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            shareDayList = session.getMapper(UserShareDayMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return shareDayList;
    }

    @Override
    public int insertUserShareDay(UserShareDay userShareDay) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userShareDay.setCreateTime(st);
                userShareDay.setUpdateTime(st);

                int r = session.getMapper(UserShareDayMapper.class).insertSelective(userShareDay);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userShareDay));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<UserShareSum> selectUserShareSum(UserShareSumExample example) {
        List<UserShareSum> shareSumList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            shareSumList = session.getMapper(UserShareSumMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return shareSumList;
    }

    @Override
    public int insertUserShareSum(UserShareSum userShareSum) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                userShareSum.setCreateTime(st);
                userShareSum.setUpdateTime(st);

                int r = session.getMapper(UserShareSumMapper.class).insertSelective(userShareSum);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(userShareSum));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public UserShareSum selectUserShareSum(long userId) {
        UserShareSum shareSum = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            shareSum = session.getMapper(UserShareSumMapper.class).selectByPrimaryKey(userId);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return shareSum;
    }

    @Override
    public long countUserShareDay(UserShareDayExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(UserShareDayMapper.class).countByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return -1L;
    }

    @Override
    public OpenOnline selectOpenOnline(long userId) {
        OpenOnline online = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            online = session.getMapper(OpenOnlineMapper.class).selectByPrimaryKey(userId);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return online;
    }

    @Override
    public int insertOpenOnline(OpenOnline online) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                online.setCreateTime(st);
                online.setUpdateTime(st);

                int r = session.getMapper(OpenOnlineMapper.class).insertSelective(online);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(online));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateOpenOnline(OpenOnline online) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                online.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(OpenOnlineMapper.class).updateByPrimaryKeySelective(online);
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
    public List<OpenOnline> selectOpenOnline(OpenOnlineExample example) {
        List<OpenOnline> onlineList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            onlineList = session.getMapper(OpenOnlineMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return onlineList;
    }

    @Override
    public int updateOpenOnlineSelective(OpenOnline online, OpenOnlineExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                online.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(OpenOnlineMapper.class).updateByExampleSelective(online, example);
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
    public UserDeveloper selectUserDeveloper(long userId) {
        String json = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_PERSONAL_ + userId);
        if (!StringUtil.isEmpty(json)) {
            return GsonUtil.fromJson(json, UserDeveloper.class);
        }

        UserDeveloper developer = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            developer = session.getMapper(UserDeveloperMapper.class).selectByPrimaryKey(userId);
            if (developer != null) {
                setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_PERSONAL_ + userId, GsonUtil.toJson(developer), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return developer;
    }

    @Override
    public int insertUserDeveloper(UserDeveloper developer) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                developer.setCreateTime(st);
                developer.setUpdateTime(st);

                int r = session.getMapper(UserDeveloperMapper.class).insertSelective(developer);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(developer));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateUserDeveloper(UserDeveloper developer) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                developer.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserDeveloperMapper.class).updateByPrimaryKeySelective(developer);
                session.commit();
                if (r > 0) {
                    deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_PERSONAL_ + developer.getUserId());
                }
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public List<UserDeveloper> selectUserDeveloper(UserDeveloperExample example) {
        List<UserDeveloper> developers = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            developers = session.getMapper(UserDeveloperMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return developers;
    }

    @Override
    public int updateUserDeveloperSelective(UserDeveloper developer, UserDeveloperExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                developer.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(UserDeveloperMapper.class).updateByExampleSelective(developer, example);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }
}
