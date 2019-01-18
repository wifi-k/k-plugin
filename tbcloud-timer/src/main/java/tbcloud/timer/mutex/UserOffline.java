package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
import tbcloud.user.model.UserOnline;
import tbcloud.user.model.UserOnlineExample;

/**
 * 在线时长超过24小时，且token不存在的用户(超过token有效期)，设置为离线状态
 * <p>
 * 消耗性能，暂时不要  // TODO 改成在凌晨执行
 *
 * @author dzh
 * @date 2018-12-13 19:57
 */
public class UserOffline extends MutexTimer {

    @Override
    protected int doTimer() {
        long ts = System.currentTimeMillis();
        UserOnlineExample example = new UserOnlineExample();
        example.createCriteria().andStatusEqualTo(ApiConst.IS_ONLINE).andOnlineTimeLessThan(ts - ApiConst.REDIS_EXPIRED_24H * 1000);

        UserOnline offline = new UserOnline();
        offline.setOfflineTime(ts);
        offline.setStatus(ApiConst.IS_OFFLINE);
        int count = UserDao.updateUserOnlineSelective(offline, example);
        LOG.info("user offline {}", count);

//            example.setOrderByClause("online_time limit 100");
//            List<UserOnline> list = UserDao.selectUserOnline(example);
//            list.forEach(online -> { //TODO pipeline and batch
//                String token = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + online.getUserId());
//                if (StringUtil.isEmpty(token)) {
//                    // set offline
//                    UserOnline updated = new UserOnline();
//                    updated.setUserId(online.getUserId());
//                    updated.setStatus(ApiConst.IS_OFFLINE);
//                    updated.setOfflineTime(System.currentTimeMillis());
//
//                    UserDao.updateUserOnline(updated);
//                    LOG.info("{} offline", online.getUserId());
//
//                    try {
//                        Thread.sleep(10);
//                    } catch (InterruptedException e) {
//                        LOG.warn(e.getMessage(), e);
//                    }
//                }
//            });
        return count;
    }

    @Override
    protected String path() {
        return "/user/info/offline";
    }

    @Override
    protected long delayMs() {
        return 3600 * 1000;
    }

    @Override
    protected String name() {
        return "UserOffline";
    }
}
