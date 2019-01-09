package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-20 15:26
 */
public class NodeListReq extends PageReq {

    private String nodeId;

    private Integer status = -1;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
