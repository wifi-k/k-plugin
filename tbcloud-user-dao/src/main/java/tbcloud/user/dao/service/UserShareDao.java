package tbcloud.user.dao.service;

import tbcloud.user.model.*;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-21 14:16
 */
public interface UserShareDao {

    List<UserShareRecord> selectUserShareRecord(UserShareRecordExample example);

    int insertUserShareRecord(UserShareRecord userShareRecord);

    List<UserShareDay> selectUserShareDay(UserShareDayExample example);

    int insertUserShareDay(UserShareDay userShareDay);

    List<UserShareSum> selectUserShareSum(UserShareSumExample example);

    int insertUserShareSum(UserShareSum userShareSum);

    UserShareSum selectUserShareSum(long userId);

    long countUserShareDay(UserShareDayExample example);

}
