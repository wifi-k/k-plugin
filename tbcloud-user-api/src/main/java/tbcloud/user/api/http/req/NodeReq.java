package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-03-21 15:48
 */
public class NodeReq extends PageReq {

    private String nodeId;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
