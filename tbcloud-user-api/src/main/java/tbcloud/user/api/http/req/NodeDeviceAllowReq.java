package tbcloud.user.api.http.req;

import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.model.NodeDeviceAllow;

import java.util.List;

/**
 * @author dzh
 * @date 2019-04-02 14:52
 */
public class NodeDeviceAllowReq {
    private Long id;
    private String name;
    private String nodeId;
    private List<String> mac;
    private String st;
    private String et;
    private Integer wt;
    private Integer op;

    public NodeDeviceAllow toNodeDeviceAllow() {
        NodeDeviceAllow device = new NodeDeviceAllow();
        device.setId(id);
        device.setName(name);
        device.setNodeId(nodeId);
        if (mac != null) {
            device.setMac(GsonUtil.toJson(mac));
        }
        device.setSt(st);
        device.setEt(et);
        device.setWt(wt);
        device.setOp(op);
        return device;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<String> getMac() {
        return mac;
    }

    public void setMac(List<String> mac) {
        this.mac = mac;
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

    public String getEt() {
        return et;
    }

    public void setEt(String et) {
        this.et = et;
    }

    public Integer getWt() {
        return wt;
    }

    public void setWt(Integer wt) {
        this.wt = wt;
    }

    public Integer getOp() {
        return op;
    }

    public void setOp(Integer op) {
        this.op = op;
    }
}
