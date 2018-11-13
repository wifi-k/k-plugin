package tbcloud.user.api.http.rsp;

import tbcloud.node.model.NodeInfo;

/**
 * @author dzh
 * @date 2018-11-12 17:58
 */
public class NodeRsp extends NodeInfo {

    private String firmwareUpgrade;

    public String getFirmwareUpgrade() {
        return firmwareUpgrade;
    }

    public void setFirmwareUpgrade(String firmwareUpgrade) {
        this.firmwareUpgrade = firmwareUpgrade;
    }
}
