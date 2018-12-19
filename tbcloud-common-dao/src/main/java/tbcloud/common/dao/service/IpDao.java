package tbcloud.common.dao.service;

import tbcloud.common.model.IpInfo;
import tbcloud.common.model.IpInfoExample;

import java.util.List;

/**
 * @author dzh
 * @date 2018-12-13 16:13
 */
public interface IpDao {

    List<IpInfo> selectIpInfo(IpInfoExample example);

    IpInfo selectIpInfo(String ip);

    int insertIpInfo(IpInfo ipInfo);

    int updateIpInfo(IpInfo ipInfo);
}
