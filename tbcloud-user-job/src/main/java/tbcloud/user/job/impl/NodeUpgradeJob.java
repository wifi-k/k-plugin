package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.node.model.NodeFirmware;
import tbcloud.node.model.NodeFirmwareExample;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.util.NodeUtil;
import tbcloud.node.protocol.data.ins.FirmwareUpgrade;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.user.job.InsJob;

import java.util.List;

/**
 * @author dzh
 * @date 2019-03-21 16:23
 */
public class NodeUpgradeJob extends InsJob {
    @Override
    public int msgType() {
        return MsgType.NODE_FIRMWARE_UPGRADE;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        String nodeId = String.valueOf(val);
        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        String model = nodeInfo.getModel();

        NodeFirmwareExample firmwareExample = new NodeFirmwareExample();
        firmwareExample.createCriteria().andModelEqualTo(model).andStartTimeGreaterThanOrEqualTo(System.currentTimeMillis())
                .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        firmwareExample.setOrderByClause("start_time desc limit 1");
        List<NodeFirmware> latestFirmware = NodeDao.selectNodeFirmware(firmwareExample);
        if (latestFirmware == null || latestFirmware.size() <= 0) {
            return;
        }
        NodeFirmware firmware = latestFirmware.get(0);
        if (NodeUtil.compareFireware(firmware.getFirmware(), nodeInfo.getFirmware()) < 1) {
            return;
        }

        // 正在升级的提示, 查询指令表
//        NodeInsExample insExample = new NodeInsExample();
//        insExample.createCriteria().andNodeIdEqualTo(nodeId).andInsEqualTo(Ins.INS_FIRMWAREUPGRADE)
//                .andStatusLessThanOrEqualTo(NodeConst.INS_STATUS_RECV).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
//        List<NodeIns> list = NodeDao.selectNodeIns(insExample);
//        if (list != null && !list.isEmpty()) {
//            return ;
//        }

        FirmwareUpgrade insVal = new FirmwareUpgrade();
        insVal.setFirmware(firmware.getFirmware());
        insVal.setDownload(firmware.getDownload());

        Ins ins = new Ins();
        ins.setId(IDUtil.genInsId(nodeId, Ins.INS_FIRMWAREUPGRADE));
        ins.setIns(Ins.INS_FIRMWAREUPGRADE);
        ins.setVal(GsonUtil.toJson(insVal));

        saveThenSend(nodeId, ins);
    }

}
