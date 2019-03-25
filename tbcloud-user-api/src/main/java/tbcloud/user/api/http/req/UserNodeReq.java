package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-03-25 16:02
 */
public class UserNodeReq {

    private String inviteCode; //节点邀请码
    private String nodeId;
    private Long userId;
    private String userName;

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
