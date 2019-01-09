package tbcloud.user.api.http.req;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-20 15:26
 */
public class NodeListReq extends PageReq {

    private String nodeId;

    private List<Integer> status;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<Integer> getStatus() {
        return status;
    }

    public void setStatus(List<Integer> status) {
        this.status = status;
    }
}
