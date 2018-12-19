package tbcloud.common.dao.service;

import jframe.core.plugin.annotation.Service;

/**
 * @author dzh
 * @date 2018-12-13 16:12
 */
@Service(clazz = "tbcloud.common.dao.service.impl.CommonDaoServiceImpl", id = "tbcloud.service.common.dao")
public interface CommonDaoService extends IpDao, AreaDao {

}
