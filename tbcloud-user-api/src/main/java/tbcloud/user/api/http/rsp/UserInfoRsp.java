package tbcloud.user.api.http.rsp;

import tbcloud.user.model.UserInfo;

/**
 * @author dzh
 * @date 2019-03-25 17:39
 */
public class UserInfoRsp {

    private UserInfo user;

    private int nodeSize;

    public int getNodeSize() {
        return nodeSize;
    }

    public void setNodeSize(int nodeSize) {
        this.nodeSize = nodeSize;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }
}
