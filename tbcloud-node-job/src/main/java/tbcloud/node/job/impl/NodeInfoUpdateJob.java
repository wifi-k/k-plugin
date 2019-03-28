package tbcloud.node.job.impl;

import com.google.gson.reflect.TypeToken;
import jframe.core.msg.Msg;
import tbcloud.common.model.IpInfo;
import tbcloud.lib.api.msg.MsgMeta;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.job.NodeJob;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeWifi;
import tbcloud.node.model.NodeWifiExample;

import java.util.List;

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

        // ssid  [{"freq":1, "ssid":"","rssi":-60}, {"freq":2, "ssid":"","rssi":-60}]
        String ssidList = (String) msg.getMeta(MsgMeta.Node_Ssid_List);
        if (!StringUtil.isEmpty(ssidList)) {
            List<NodeWifi> nodeWifiList = GsonUtil.fromJson(ssidList, new TypeToken<List<NodeWifi>>() {
            }.getType());
            for (NodeWifi nodeWifi : nodeWifiList) {
                nodeWifi.setNodeId(nodeInfo.getNodeId());
                if (nodeWifi.getFreq() == null) {
                    LOG.warn("miss NodeWifi {}", GsonUtil.toJson(nodeWifi));
                    continue;
                }

                NodeWifiExample example = new NodeWifiExample();
                example.createCriteria().andNodeIdEqualTo(nodeWifi.getNodeId()).andFreqEqualTo(nodeWifi.getFreq());

                List<NodeWifi> list = NodeDao.selectNodeWifi(example);
                if (list == null || list.isEmpty()) {
                    NodeDao.insertNodeWifi(nodeWifi);
                } else {
                    nodeWifi.setId(list.get(0).getId());
                    NodeDao.updateNodeWifi(nodeWifi);
                }
            }
        }

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

}
