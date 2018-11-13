package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-11 23:12
 */
public class SignUpPasswdReq {

    private String mobile;
    private String passwd;
    private String vcode;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getVcode() {
        return vcode;
    }

    public void setVcode(String vcode) {
        this.vcode = vcode;
    }
}
