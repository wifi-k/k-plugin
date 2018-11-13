package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-12 02:25
 */
public class PasswdForgetReq {

    private String mobile;
    private String vcode;
    private String passwd;

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

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }
}
