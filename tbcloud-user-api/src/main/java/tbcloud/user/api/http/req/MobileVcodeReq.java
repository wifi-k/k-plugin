package tbcloud.user.api.http.req;

import tbcloud.lib.api.util.GsonUtil;

/**
 * @author dzh
 * @date 2018-11-11 22:13
 */
public class MobileVcodeReq {

    private Integer type;
    private String mobile;
    private String imgCodeId;
    private String imgCode;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
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

    @Override
    public String toString() {
        return GsonUtil.toJson(this);
    }
}
