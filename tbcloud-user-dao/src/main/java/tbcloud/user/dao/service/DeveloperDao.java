package tbcloud.user.dao.service;

import tbcloud.user.model.OpenOnline;
import tbcloud.user.model.OpenOnlineExample;
import tbcloud.user.model.UserDeveloper;
import tbcloud.user.model.UserDeveloperExample;

import java.util.List;

/**
 * 个人/企业用户认证
 *
 * @author dzh
 * @date 2018-12-24 18:58
 */
public interface DeveloperDao {

    OpenOnline selectOpenOnline(long userId);

    int insertOpenOnline(OpenOnline online);

    int updateOpenOnline(OpenOnline online);

    List<OpenOnline> selectOpenOnline(OpenOnlineExample example);

    int updateOpenOnlineSelective(OpenOnline online, OpenOnlineExample example);

    UserDeveloper selectUserDeveloper(long userId);

    int insertUserDeveloper(UserDeveloper developer);

    int updateUserDeveloper(UserDeveloper developer);

    List<UserDeveloper> selectUserDeveloper(UserDeveloperExample example);

    int updateUserDeveloperSelective(UserDeveloper developer, UserDeveloperExample example);


}
