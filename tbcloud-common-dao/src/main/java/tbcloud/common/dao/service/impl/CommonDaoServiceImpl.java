package tbcloud.common.dao.service.impl;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import jframe.mybatis.MultiMybatisService;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.common.dao.CommonDaoPlugin;
import tbcloud.common.dao.service.CommonDaoService;
import tbcloud.common.model.IpInfo;
import tbcloud.common.model.IpInfoExample;
import tbcloud.common.model.mapper.IpInfoMapper;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;

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
            try {
                ipInfoList = session.getMapper(IpInfoMapper.class).selectByExample(example);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return ipInfoList;
    }

    @Override
    public IpInfo selectIpInfo(String ip) {
        IpInfo ipInfo = null;
        try (SqlSession session = MultiMybatisSvc.getSqlSessionFactory(ApiConst.MYSQL_TBCLOUD).openSession()) {
            try {
                ipInfo = session.getMapper(IpInfoMapper.class).selectByPrimaryKey(ip);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
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
                //TODO rm cache redis
            }
        }
        return 0;
    }
}
