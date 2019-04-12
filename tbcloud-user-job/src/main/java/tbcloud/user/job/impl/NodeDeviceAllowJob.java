package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeDeviceAllow;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.protocol.data.ins.DeviceAllow;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.node.protocol.data.ins.InsVal;
import tbcloud.user.job.InsJob;

/**
 * @author dzh
 * @date 2019-04-01 12:15
 */
public class NodeDeviceAllowJob extends InsJob {
    @Override
    public int msgType() {
        return MsgType.NODE_DEVICE_ALLOW;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeDeviceAllow deviceAllow = null;
        if (val instanceof String) { // maybe from mq
            deviceAllow = GsonUtil.fromJson((String) val, NodeDeviceAllow.class);
        } else if (val instanceof NodeDeviceAllow) {
            deviceAllow = (NodeDeviceAllow) val;
        } else {
            return;
        }

        String nodeId = deviceAllow.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            LOG.error("miss nodeId {}", val);
            return;
        }

        Long id = deviceAllow.getId();
        if (id == null || id == 0) { // insert
            deviceAllow.setOp(InsVal.OP_ENABLE);
            NodeDao.insertNodeDeviceAllow(deviceAllow);
        } else {
            NodeDao.updateNodeDeviceAllow(deviceAllow);
        }

        if (deviceAllow.getOp() == null) {
            if (!StringUtil.isEmpty(deviceAllow.getMac()) || StringUtil.isEmpty(deviceAllow.getSt())) {
                deviceAllow.setOp(InsVal.OP_ENABLE);
            }
        }

        if (deviceAllow.getOp() != null) {
            // send ins
            DeviceAllow insVal = new DeviceAllow();
            insVal.setMac(deviceAllow.getMac());
            insVal.setOp(deviceAllow.getOp());
            insVal.setSt(deviceAllow.getSt());
            insVal.setEt(deviceAllow.getEt());
            insVal.setWt(deviceAllow.getWt());
            insVal.setId(deviceAllow.getId());

            Ins ins = new Ins();
            ins.setId(IDUtil.genInsId(nodeId, Ins.INS_DEVICEALLOW));
            ins.setIns(Ins.INS_DEVICEALLOW);
            ins.setVal(GsonUtil.toJson(insVal));

            // send ins
            saveThenSend(nodeId, ins);
        }
    }
}
