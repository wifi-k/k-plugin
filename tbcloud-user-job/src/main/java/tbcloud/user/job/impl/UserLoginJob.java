package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.msg.UserLogin;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.user.job.UserJob;
import tbcloud.user.model.OpenOnline;
import tbcloud.user.model.UserOnline;

/**
 * @author dzh
 * @date 2018-11-22 10:37
 */
public class UserLoginJob extends UserJob {
    @Override
    public int msgType() {
        return MsgType.LOGIN_OUT;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        UserLogin userLogin = null;
        if (val instanceof String) { // maybe from mq in the future
            userLogin = GsonUtil.fromJson((String) val, UserLogin.class);
        } else if (val instanceof UserLogin) {
            userLogin = (UserLogin) val;
        } else {
            return;
        }

        int platform = userLogin.getPlatform();
        switch (platform) {
            case ApiConst.PLATFORM_USER: {
                UserOnline online = toUserOnline(userLogin);
                if (UserDao.updateUserOnline(online) == 0) {
                    // maybe not existed
                    UserDao.insertUserOnline(online);
                }

                if (online.getStatus().intValue() == ApiConst.IS_OFFLINE) {
                    // delete token in redis
                    deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + online.getUserId());
                }
                break;
            }
            case ApiConst.PLATFORM_OPEN: {
                OpenOnline online = toOpenOnline(userLogin);
                if (UserDao.updateOpenOnline(online) == 0) {
                    // maybe not existed
                    UserDao.insertOpenOnline(online);
                }

                if (online.getStatus().intValue() == ApiConst.IS_OFFLINE) {
                    // delete token in redis
                    deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_OPEN_TOKEN_ + online.getUserId());
                }
                break;
            }
        }

    }

    private OpenOnline toOpenOnline(UserLogin userLogin) {
        int status = userLogin.getStatus();

        OpenOnline online = new OpenOnline();
        online.setUserId(userLogin.getUserId());
        online.setStatus(status);
        if (status == ApiConst.IS_ONLINE) {
            online.setOnlineTime(userLogin.getDate());
            online.setToken(userLogin.getToken());
        } else {
            online.setOfflineTime(userLogin.getDate());
        }
        return online;
    }

    private UserOnline toUserOnline(UserLogin userLogin) {
        int status = userLogin.getStatus();

        UserOnline online = new UserOnline();
        online.setUserId(userLogin.getUserId());
        online.setStatus(status);

        online.setDevToken(userLogin.getToken());
        online.setDevType(userLogin.getDevType());
        online.setDevOs(userLogin.getDevOs());

        if (status == ApiConst.IS_ONLINE) {
            online.setOnlineTime(userLogin.getDate());
            online.setToken(userLogin.getToken());
        } else {
            online.setOfflineTime(userLogin.getDate());
        }
        return online;
    }

}
