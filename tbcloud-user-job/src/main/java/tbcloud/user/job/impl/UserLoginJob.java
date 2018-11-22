package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.msg.UserLogin;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.user.job.UserJob;
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
        }

        UserOnline userOnline = toUserOnline(userLogin);
        if (UserDao.updateUserOnline(userOnline) == 0) {
            // maybe not existed
            UserDao.insertUserOnline(userOnline);
        }
    }

    private UserOnline toUserOnline(UserLogin userLogin) {
        int status = userLogin.getStatus();

        UserOnline userOnline = new UserOnline();
        userOnline.setUserId(userLogin.getUserId());
        userOnline.setStatus(status);
        if (status == ApiConst.USER_ONLINE) {
            userOnline.setOnlineTime(userLogin.getDate());
            userOnline.setToken(userLogin.getToken());
        } else {
            userOnline.setOfflineTime(userLogin.getDate());
        }
        return userOnline;
    }

    @Override
    protected String id() {
        return "userlogin";
    }
}
