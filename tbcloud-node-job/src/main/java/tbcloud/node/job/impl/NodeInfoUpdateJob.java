package tbcloud.node.job.impl;

import jframe.core.msg.Msg;
import tbcloud.common.model.IpInfo;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.job.NodeJob;
import tbcloud.node.model.NodeInfo;

/**
 * @author dzh
 * @date 2019-01-18 14:59
 */
public class NodeInfoUpdateJob extends NodeJob {


    @Override
    public int msgType() {
        return MsgType.NODE_INFO_UPDATE;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeInfo nodeInfo = null;
        if (val instanceof String) {  // from mq
            nodeInfo = GsonUtil.fromJson((String) val, NodeInfo.class);
        } else if (val instanceof NodeInfo) {
            nodeInfo = (NodeInfo) val;
        }

        if (nodeInfo == null)
            return;

        NodeDao.updateNodeInfo(nodeInfo);

        // ip
        String ip = nodeInfo.getIp();
        if (!StringUtil.isEmpty(ip)) {
            IpInfo ipInfo = CommonDao.selectIpInfo(ip);
            if (ipInfo == null) {
                ipInfo = new IpInfo();
                ipInfo.setIp(ip);
                CommonDao.insertIpInfo(ipInfo);
                LOG.info("find new ip {}", ip);
            }
        }
    }

    @Override
    protected String id() {
        return "NodeInfoUpdateJob";
    }
}
