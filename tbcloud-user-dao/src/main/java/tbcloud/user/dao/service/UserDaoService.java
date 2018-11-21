package tbcloud.user.dao.service;

import jframe.core.plugin.annotation.Service;

/**
 * @author dzh
 * @date 2018-11-09 11:55
 */
@Service(clazz = "tbcloud.user.dao.service.impl.UserDaoServiceImpl", id = "tbcloud.service.user.dao")
public interface UserDaoService extends UserDao, ImgCodeDao, UserShareDao {


}
