package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-12 02:07
 */
public class SignInPasswdReq {

    private String mobile;
    private String passwd;
    private String imgCodeId;
    private String imgCode;

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

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getImgCodeId() {
        return imgCodeId;
    }

    public void setImgCodeId(String imgCodeId) {
        this.imgCodeId = imgCodeId;
    }

    public String getImgCode() {
        return imgCode;
    }

    public void setImgCode(String imgCode) {
        this.imgCode = imgCode;
    }
}
