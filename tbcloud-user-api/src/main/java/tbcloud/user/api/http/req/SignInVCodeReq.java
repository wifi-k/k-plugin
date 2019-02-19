package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-02-19 14:48
 */
public class SignInVCodeReq {

    private String mobile;
    private String vcode;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getVcode() {
        return vcode;
    }

    public void setVcode(String vcode) {
        this.vcode = vcode;
    }
}
