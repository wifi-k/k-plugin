package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeWifiTimer;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.node.protocol.data.ins.WifiTimer;
import tbcloud.user.job.InsJob;

/**
 * @author dzh
 * @date 2019-03-27 12:01
 */
public class NodeWifiTimerJob extends InsJob {

    @Override
    public int msgType() {
        return MsgType.NODE_WIFI_TIMER;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeWifiTimer timer = null;
        if (val instanceof String) { // maybe from mq
            timer = GsonUtil.fromJson((String) val, NodeWifiTimer.class);
        } else if (val instanceof NodeWifiTimer) {
            timer = (NodeWifiTimer) val;
        } else {
            return;
        }

        String nodeId = timer.getNodeId();
        if (StringUtil.isEmpty(nodeId)) return;

        timer = NodeDao.selectNodeWifiTimer(timer.getNodeId());
        if (timer == null) { //insert
            NodeDao.insertNodeWifiTimer(timer);
        } else {
            NodeDao.updateNodeWifiTimer(timer);
        }


        WifiTimer insVal = new WifiTimer();
        insVal.setOp(timer.getOp());
        insVal.setWifi(timer.getWifi());

        Ins ins = new Ins();
        ins.setId(IDUtil.genInsId(nodeId, Ins.INS_WIFITIMER));
        ins.setIns(Ins.INS_WIFITIMER);
        ins.setVal(GsonUtil.toJson(insVal));

        // send ins
        saveThenSend(nodeId, ins);
    }


}
