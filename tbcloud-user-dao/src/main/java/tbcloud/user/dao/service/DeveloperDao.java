package tbcloud.user.dao.service;

import tbcloud.user.model.OpenOnline;
import tbcloud.user.model.OpenOnlineExample;

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

}
