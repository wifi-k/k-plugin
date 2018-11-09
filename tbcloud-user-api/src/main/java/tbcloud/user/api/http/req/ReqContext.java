package tbcloud.user.api.http.req;

import tbcloud.user.model.UserInfo;

/**
 * @author dzh
 * @date 2018-11-08 21:34
 */
public class ReqContext {

    private String version;
    private String token;
    private UserInfo userInfo;

    public static final ReqContext create(String version, String token) {
        ReqContext req = new ReqContext();
        req.version = version;
        req.token = token;
        return req;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
