package tbcloud.user.dao.service;

import tbcloud.user.model.UserInfo;
import tbcloud.user.model.UserInfoExample;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-09 11:55
 */
public interface UserDao {

    List<UserInfo> selectUserInfo(UserInfoExample example);

    UserInfo selectUserInfo(long userId);

    int insertUserInfo(UserInfo userInfo);

    int updateUserInfo(UserInfo userInfo);

}
