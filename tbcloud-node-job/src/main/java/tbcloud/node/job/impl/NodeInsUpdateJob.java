package tbcloud.node.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.job.NodeJob;
import tbcloud.node.model.NodeIns;

/**
 * @author dzh
 * @date 2019-01-18 15:55
 */
public class NodeInsUpdateJob extends NodeJob {
    @Override
    public int msgType() {
        return MsgType.NODE_INS_UPDATE;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeIns nodeIns = null;
        if (val instanceof String) {  // from mq
            nodeIns = GsonUtil.fromJson((String) val, NodeIns.class);
        } else if (val instanceof NodeIns) {
            nodeIns = (NodeIns) val;
        }

        if (nodeIns == null) return;

        NodeDao.updateNodeIns(nodeIns);
    }

}
