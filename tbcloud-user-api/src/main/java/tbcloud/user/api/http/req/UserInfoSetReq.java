package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-12 09:59
 */
public class UserInfoSetReq {

    private String name;
    private String avatar;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
