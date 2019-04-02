package tbcloud.user.api.http.rsp;

import com.google.gson.reflect.TypeToken;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeDeviceAllow;

import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2019-04-02 15:01
 */
public class NodeDeviceAllowRsp {

    private Long id;
    private String name;
    private String nodeId;
    private List<String> mac;
    private String st;
    private String et;
    private Integer wt;
    private Integer op;

    public static NodeDeviceAllowRsp from(NodeDeviceAllow allow) {
        NodeDeviceAllowRsp rsp = new NodeDeviceAllowRsp();
        rsp.setId(allow.getId());
        rsp.setName(allow.getName());
        rsp.setNodeId(allow.getNodeId());
        rsp.setMac(StringUtil.isEmpty(allow.getMac()) ? Collections.emptyList() :
                GsonUtil.fromJson(allow.getMac(), new TypeToken<List<String>>() {
                }.getType()));
        rsp.setSt(allow.getSt());
        rsp.setEt(allow.getEt());
        rsp.setWt(allow.getWt());
        rsp.setOp(allow.getOp());
        return rsp;
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
