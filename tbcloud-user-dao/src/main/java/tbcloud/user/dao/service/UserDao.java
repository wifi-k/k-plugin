package tbcloud.user.dao.service;

import tbcloud.user.model.*;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-09 11:55
 */
public interface UserDao {

    List<UserNode> selectUserNode(UserNodeExample example);

    UserNode selectUserNode(long userId);

    int insertUserNode(UserNode userNode);

    int updateUserNode(UserNode userNode);

    void batchUpdateUserNode(List<UserNode> userNode);

    int updateUserNodeSelective(UserNode userNode, UserNodeExample example);

    long countUserNode(UserNodeExample example);

    List<UserMessage> selectUserMessage(UserMessageExample example);

    UserMessage selectUserMessage(long id);

    int batchInsertUserMessage(List<UserMessage> message);

    int insertUserMessage(UserMessage message);

    int updateUserMessage(UserMessage message);


    List<UserInfo> selectUserInfo(UserInfoExample example);

    UserInfo selectUserInfo(long userId);

    int insertUserInfo(UserInfo userInfo);

    int updateUserInfo(UserInfo userInfo);


    UserOnline selectUserOnline(long userId);

    int insertUserOnline(UserOnline userOnline);

    int updateUserOnline(UserOnline userOnline);

    List<UserOnline> selectUserOnline(UserOnlineExample example);

    int updateUserOnlineSelective(UserOnline userOnline, UserOnlineExample example);


}
