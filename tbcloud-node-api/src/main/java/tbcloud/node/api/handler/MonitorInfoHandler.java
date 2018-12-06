package tbcloud.node.api.handler;

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

        // TODO async
        // insert es

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

            int status = NodeConst.STATUS_NORMAIL;
            // status TODO config
            if (cpuUsage > 80 || memUsage > 80 || diskUsage > 80) {
                status = NodeConst.STATUS_WARN;
            }
            if (cpuUsage > 95 || memUsage > 95 || diskUsage > 95) {
                status = NodeConst.STATUS_ERROR;
                // TODO alert
            }
            nodeRt.setStatus(status);

            NodeDao.updateNodeRt(nodeRt);
        }

        DataRsp<Void> rsp = new DataRsp<>();
        return rsp;
    }

    @Override
    protected MonitorInfo decodeDataReq(IoContext context) {
        return context.dataCodec().decode(context.request().data(), MonitorInfo.class);
    }
}
