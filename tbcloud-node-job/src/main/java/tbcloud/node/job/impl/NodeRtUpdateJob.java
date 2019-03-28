package tbcloud.node.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.job.NodeJob;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeRt;

/**
 * @author dzh
 * @date 2019-01-18 15:14
 */
public class NodeRtUpdateJob extends NodeJob {
    @Override
    public int msgType() {
        return MsgType.NODE_RT_UPDATE;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeRt nodeRt = null;
        if (val instanceof String) {  // from mq
            nodeRt = GsonUtil.fromJson((String) val, NodeRt.class);
        } else if (val instanceof NodeRt) {
            nodeRt = (NodeRt) val;
        }

        if (nodeRt == null) return;
        String nodeId = nodeRt.getNodeId();
        if (StringUtil.isEmpty(nodeId)) return; //TODO error

        Integer status = nodeRt.getStatus();
        if (status != null && status > NodeConst.STATUS_OFFLINE) { // online
            Integer cpuUsage = nodeRt.getCpuUsage();
            Integer memUsage = nodeRt.getMemUsage();
            Integer diskUsage = nodeRt.getDiskUsage();

            // calc health TODO alert user
            nodeRt.setHealth(NodeConst.HEALTH_NORMAL);
            if ((cpuUsage != null && cpuUsage > 80) || (memUsage != null && memUsage > 80)
                    || (diskUsage != null && diskUsage > 80)) {
                nodeRt.setHealth(NodeConst.HEALTH_WARN);
            }
            if ((cpuUsage != null && cpuUsage > 90) || (memUsage != null && memUsage > 95)
                    || (diskUsage != null && diskUsage > 95)) {
                nodeRt.setHealth(NodeConst.HEALTH_ERROR);
            }
        }
        NodeDao.updateNodeRt(nodeRt);
    }

}
