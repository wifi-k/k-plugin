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
            try {
                imgCode = session.getMapper(UserImgCodeMapper.class).selectByPrimaryKey(id);
                if (imgCode != null) {  //TODO async
                    setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_IMGCODE_ + id, GsonUtil.toJson(imgCode), ApiConst.REDIS_EXPIRED_1H);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return imgCode;
    }


    private String getFromRedis(String id, String key) {
        Jedis jedis = null;
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

    @Override
    public List<UserInfo> selectUserInfo(UserInfoExample example) {
        List<UserInfo> userInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                userInfoList = session.getMapper(UserInfoMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return userInfoList;
    }

    @Override
    public UserInfo selectUserInfo(long userId) { //TODO cache
        UserInfo userInfo = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                userInfo = session.getMapper(UserInfoMapper.class).selectByPrimaryKey(userId);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
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
    public List<UserShareRecord> selectUserShareRecord(UserShareRecordExample example) {
        List<UserShareRecord> shareRecordList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                shareRecordList = session.getMapper(UserShareRecordMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
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
            try {
                shareDayList = session.getMapper(UserShareDayMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
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
            try {
                shareSumList = session.getMapper(UserShareSumMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
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
            try {
                shareSum = session.getMapper(UserShareSumMapper.class).selectByPrimaryKey(userId);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return shareSum;
    }

    @Override
    public long countUserShareDay(UserShareDayExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                return session.getMapper(UserShareDayMapper.class).countByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return -1L;
    }
}
