package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-02-19 14:48
 */
public class SignInVCodeReq {

    private String mobile;
    private String vcode;
    private String devToken;
    private Integer devType;
    private String devOs;

    public String getDevToken() {
        return devToken;
    }

    public void setDevToken(String devToken) {
        this.devToken = devToken;
    }

    public Integer getDevType() {
        return devType;
    }

    public void setDevType(Integer devType) {
        this.devType = devType;
    }

    public String getDevOs() {
        return devOs;
    }

    public void setDevOs(String devOs) {
        this.devOs = devOs;
    }

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
