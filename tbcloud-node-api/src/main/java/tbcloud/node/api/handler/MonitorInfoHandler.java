package tbcloud.node.api.handler;

import jframe.core.msg.TextMsg;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeRt;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.data.DataRsp;
import tbcloud.node.protocol.data.MonitorInfo;

/**
 * @author dzh
 * @date 2018-11-28 19:13
 */
public class MonitorInfoHandler extends DataHandler<MonitorInfo> {

    public MonitorInfoHandler(IoContext context) {
        super(context);
    }

    @Override
    protected DataRsp<Void> handle(MonitorInfo dataReq) {
        String nodeId = dataReq.getNodeId();

        if (dataReq.getTakeTime() < 1) dataReq.setTakeTime(System.currentTimeMillis());

        // update node_rt
        if (dataReq.getType() == PacketConst.MONITOR_TYPE_SYS) {
            NodeRt nodeRt = new NodeRt();
            nodeRt.setNodeId(nodeId);

            int cpuUsage = dataReq.getCpuUsage();
            int memUsage = dataReq.getMemUsage();
            int diskUsage = dataReq.getDiskUsage();

            nodeRt.setCpuUsage(cpuUsage);
            nodeRt.setMemUsage(memUsage);
            nodeRt.setDiskUsage(diskUsage);
            nodeRt.setTakeTime(dataReq.getTakeTime());
            nodeRt.setStatus(NodeConst.STATUS_NORMAIL);

//            NodeDao.updateNodeRt(nodeRt);
            Plugin.sendToNode(new TextMsg().setType(MsgType.NODE_RT_UPDATE).setValue(GsonUtil.toJson(nodeRt)), nodeId);
        }

        return SUCC;
    }

    @Override
    protected MonitorInfo decodeDataReq(IoContext context) {
        return context.dataCodec().decode(context.request().data(), MonitorInfo.class);
    }
}
