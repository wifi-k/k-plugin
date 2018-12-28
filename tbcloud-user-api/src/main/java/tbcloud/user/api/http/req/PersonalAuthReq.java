package tbcloud.user.api.http.req;

import tbcloud.lib.api.util.StringUtil;
import tbcloud.user.model.UserDeveloper;

/**
 * 个人审核
 *
 * @author dzh
 * @date 2018-12-25 10:06
 */
public class PersonalAuthReq {
    private String name;
    private String idNum;
    private String imgIdBack;
    private String imgIdUser;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdNum() {
        return idNum;
    }

    public void setIdNum(String idNum) {
        this.idNum = idNum;
    }

    public String getImgIdBack() {
        return imgIdBack;
    }

    public void setImgIdBack(String imgIdBack) {
        this.imgIdBack = imgIdBack;
    }

    public String getImgIdUser() {
        return imgIdUser;
    }

    public void setImgIdUser(String imgIdUser) {
        this.imgIdUser = imgIdUser;
    }

    public UserDeveloper toDeveloper(long userId) {
        UserDeveloper developer = new UserDeveloper();
        developer.setUserId(userId);
        developer.setIdNum(idNum);
        developer.setImgIdBack(imgIdBack);
        developer.setImgIdUser(imgIdUser);
        return developer;
    }

    /**
     * 缺少参数检查
     *
     * @return
     */
    public boolean isMissing() {
        return StringUtil.isEmpty(name) && StringUtil.isEmpty(idNum) && StringUtil.isEmpty(imgIdBack) && StringUtil.isEmpty(imgIdUser);
    }
}
