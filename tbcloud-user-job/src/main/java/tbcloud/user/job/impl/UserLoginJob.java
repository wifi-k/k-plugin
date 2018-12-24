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
        return MsgType.USER_LOGIN;
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
                UserOnline userOnline = toUserOnline(userLogin);
                if (UserDao.updateUserOnline(userOnline) == 0) {
                    // maybe not existed
                    UserDao.insertUserOnline(userOnline);
                }
                break;
            }
            case ApiConst.PLATFORM_OPEN: {
                OpenOnline online = toOpenOnline(userLogin);
                if (UserDao.updateOpenOnline(online) == 0) {
                    // maybe not existed
                    UserDao.insertOpenOnline(online);
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
        if (status == ApiConst.IS_ONLINE) {
            online.setOnlineTime(userLogin.getDate());
            online.setToken(userLogin.getToken());
        } else {
            online.setOfflineTime(userLogin.getDate());
        }
        return online;
    }

    @Override
    protected String id() {
        return "userlogin";
    }
}
