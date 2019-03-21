package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.*;
import tbcloud.node.model.util.NodeUtil;
import tbcloud.node.protocol.data.ins.FirmwareUpgrade;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.user.job.UserJob;

import java.util.List;

/**
 * @author dzh
 * @date 2019-03-21 16:23
 */
public class NodeUpgradeJob extends UserJob {
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

        NodeIns nodeIns = new NodeIns();
        nodeIns.setId(ins.getId());
        nodeIns.setNodeId(nodeId);
        nodeIns.setIns(ins.getIns());
        nodeIns.setStatus(NodeConst.INS_STATUS_SEND);
        nodeIns.setVal(ins.getVal());
        nodeIns.setSendTime(System.currentTimeMillis());
        nodeIns.setRetry(0);
        NodeDao.insertNodeIns(nodeIns);

        // send ins
        addToRedis(nodeId, ins);
    }

    boolean addToRedis(String nodeId, Ins ins) { //TODO mq 在一个地方统一发送,一个节点的发送保持有序
        if (StringUtil.isEmpty(nodeId) || ins == null) {
            LOG.error("nodeId is nil");
            return false;
        }
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(ApiConst.REDIS_ID_NODE)) { //TODO 并行指令、指令预测
            long count = jedis.scard(ApiConst.REDIS_KEY_NODE_INS_ + nodeId);
            if (count == 0) {
                jedis.sadd(ApiConst.REDIS_KEY_NODE_INS_ + nodeId, GsonUtil.toJson(ins));
                LOG.info("node {}, add ins {} {} {}", nodeId, ins.getId(), ins.getIns(), ins.getVal());
                return true;
            }
        }
        return false;
    }

}
