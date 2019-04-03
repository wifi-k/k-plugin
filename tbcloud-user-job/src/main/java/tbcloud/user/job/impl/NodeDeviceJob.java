package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeDevice;
import tbcloud.node.model.NodeDeviceExample;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.protocol.data.ins.DeviceBlock;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.user.job.InsJob;

/**
 * @author dzh
 * @date 2019-03-28 16:50
 */
public class NodeDeviceJob extends InsJob {

    @Override
    public int msgType() {
        return MsgType.NODE_DEVICE_BLOCK;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeDevice device = null;
        if (val instanceof String) { // maybe from mq
            device = GsonUtil.fromJson((String) val, NodeDevice.class);
        } else if (val instanceof NodeInfo) {
            device = (NodeDevice) val;
        } else {
            return;
        }

        String nodeId = device.getNodeId();
        String mac = device.getMac();
        if (StringUtil.isEmpty(mac)) return;
        if (StringUtil.isEmpty(nodeId)) return;

        NodeDeviceExample example = new NodeDeviceExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andMacEqualTo(mac).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        NodeDao.updateNodeDeviceSelective(device, example);

        if (device.getIsBlock() != null) {
            DeviceBlock insVal = new DeviceBlock();
            insVal.setMac(device.getMac());
            insVal.setOp(device.getIsBlock());

            Ins ins = new Ins();
            ins.setId(IDUtil.genInsId(device.getNodeId(), Ins.INS_DEVICEBLOCK));
            ins.setIns(Ins.INS_DEVICEBLOCK);
            ins.setVal(GsonUtil.toJson(insVal));

            // send ins
            saveThenSend(nodeId, ins);
        }
    }
}
