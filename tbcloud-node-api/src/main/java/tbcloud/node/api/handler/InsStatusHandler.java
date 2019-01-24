package tbcloud.node.api.handler;

import jframe.core.msg.TextMsg;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeIns;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.data.DataRsp;
import tbcloud.node.protocol.data.InsStatus;

/**
 * @author dzh
 * @date 2018-11-28 19:11
 */
public class InsStatusHandler extends DataHandler<InsStatus> {
    public InsStatusHandler(IoContext context) {
        super(context);
    }

    @Override
    protected DataRsp<Void> handle(InsStatus data) {
        DataRsp<Void> rsp = new DataRsp<>();

        String nodeId = data.getNodeId();
        String insId = data.getId();
        Integer status = data.getStatus();
        if (StringUtil.isEmpty(nodeId) || StringUtil.isEmpty(insId) || status == null) {
            rsp.setCode(ApiCode.NODE_MISS_PARAM);
            return rsp;
        }

        // update node_ins
        NodeIns nodeIns = new NodeIns();
        nodeIns.setId(insId);
        // status
        int insStatus = data.getStatus();
        nodeIns.setStatus(insStatus);
        switch (insStatus) {
            case NodeConst.INS_STATUS_RECV:
                nodeIns.setRecvTime(System.currentTimeMillis());
                break;
            case NodeConst.INS_STATUS_SUCC:
                nodeIns.setEndTime(System.currentTimeMillis());
                break;
            case NodeConst.INS_STATUS_FAIL:
                nodeIns.setEndTime(System.currentTimeMillis());
                break;
        }
//        NodeDao.updateNodeIns(nodeIns);
        Plugin.sendToNode(new TextMsg().setType(MsgType.NODE_INS_UPDATE).setValue(GsonUtil.toJson(nodeIns)), nodeId);
        return rsp;
    }

    @Override
    protected InsStatus decodeDataReq(IoContext context) {
        LOG.info("data {}", new String(context.request().data().array(), PacketConst.UTF_8));
        return context.dataCodec().decode(context.request().data(), InsStatus.class);
    }
}
