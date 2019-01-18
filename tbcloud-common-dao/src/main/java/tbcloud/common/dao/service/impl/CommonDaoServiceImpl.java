package tbcloud.common.dao.service.impl;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import jframe.mybatis.MultiMybatisService;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.common.dao.CommonDaoPlugin;
import tbcloud.common.dao.service.CommonDaoService;
import tbcloud.common.model.*;
import tbcloud.common.model.mapper.AreaMapper;
import tbcloud.common.model.mapper.CityMapper;
import tbcloud.common.model.mapper.IpInfoMapper;
import tbcloud.common.model.mapper.ProvinceMapper;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2018-12-13 16:13
 */
@Injector
public class CommonDaoServiceImpl implements CommonDaoService {

    static Logger LOG = LoggerFactory.getLogger(CommonDaoServiceImpl.class);

    @InjectPlugin
    static CommonDaoPlugin Plugin;

    @InjectService(id = "jframe.service.multimybatis")
    static MultiMybatisService MultiMybatisSvc;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @Override
    public List<IpInfo> selectIpInfo(IpInfoExample example) {
        List<IpInfo> ipInfoList = Collections.emptyList();
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            ipInfoList = session.getMapper(IpInfoMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return ipInfoList;
    }

    @Override
    public IpInfo selectIpInfo(String ip) {
        IpInfo ipInfo = getFromRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_IPINFO_ + ip, IpInfo.class);
        if (ipInfo != null) return ipInfo;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            ipInfo = session.getMapper(IpInfoMapper.class).selectByPrimaryKey(ip);
            if (ipInfo != null) {
                setToRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_IPINFO_ + ip, GsonUtil.toJson(ipInfo), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return ipInfo;
    }

    @Override
    public int insertIpInfo(IpInfo ipInfo) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                long st = System.currentTimeMillis();
                ipInfo.setCreateTime(st);
                ipInfo.setUpdateTime(st);

                int r = session.getMapper(IpInfoMapper.class).insertSelective(ipInfo);
                session.commit();

                LOG.info("insert {} {}", r, GsonUtil.toJson(ipInfo));
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            }
        }
        return 0;
    }

    @Override
    public int updateIpInfo(IpInfo ipInfo) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                ipInfo.setUpdateTime(System.currentTimeMillis());
                int r = session.getMapper(IpInfoMapper.class).updateByPrimaryKeySelective(ipInfo);
                session.commit();
                return r;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                session.rollback();
            } finally {
                deleteFromRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_IPINFO_ + ipInfo.getIp());
            }
        }
        return 0;
    }

    @Override
    public List<Province> selectProvince(ProvinceExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(ProvinceMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<City> selectCity(CityExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(CityMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Area> selectArea(AreaExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(AreaMapper.class).selectByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public long countProvince(ProvinceExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(ProvinceMapper.class).countByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public long countCity(CityExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(CityMapper.class).countByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public long countArea(AreaExample example) {
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            return session.getMapper(AreaMapper.class).countByExample(example);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public Province selectProvince(String provinceId) {
        Province p = getFromRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_PROVINCE_ + provinceId, Province.class);
        if (p != null) return p;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            p = session.getMapper(ProvinceMapper.class).selectByPrimaryKey(provinceId);
            if (p != null) {
                setToRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_PROVINCE_ + provinceId, GsonUtil.toJson(p), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return p;
    }

    @Override
    public City selectCity(String cityId) {
        City city = getFromRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_CITY_ + cityId, City.class);
        if (city != null) return city;

        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            city = session.getMapper(CityMapper.class).selectByPrimaryKey(cityId);
            if (city != null) {
                setToRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_CITY_ + cityId, GsonUtil.toJson(city), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return city;
    }

    @Override
    public Area selectArea(String areaId) {
        Area area = getFromRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_AREA_ + areaId, Area.class);
        if (area != null) return area;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            area = session.getMapper(AreaMapper.class).selectByPrimaryKey(areaId);
            if (area != null) {
                setToRedis(ApiConst.REDIS_ID_COMMON, ApiConst.REDIS_KEY_BASIC_AREA_ + areaId, GsonUtil.toJson(area), ApiConst.REDIS_EXPIRED_1H);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return area;
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
