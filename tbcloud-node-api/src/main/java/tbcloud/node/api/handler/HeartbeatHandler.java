package tbcloud.node.api.handler;

import jframe.core.msg.TextMsg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeRt;
import tbcloud.node.protocol.data.DataRsp;
import tbcloud.node.protocol.data.Heartbeat;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.node.protocol.data.rsp.HeartbeatRsp;

/**
 * @author dzh
 * @date 2018-11-28 19:09
 */
public class HeartbeatHandler extends DataHandler<Heartbeat> {

    public HeartbeatHandler(IoContext context) {
        super(context);
    }

    @Override
    protected DataRsp<HeartbeatRsp> handle(Heartbeat dataReq) {
        String nodeId = dataReq.getNodeId();
        DataRsp<HeartbeatRsp> dataRsp = new DataRsp<>();

        // send ins to node
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(ApiConst.REDIS_ID_NODE)) {
            String insJson = jedis.spop(ApiConst.REDIS_KEY_NODE_INS_ + nodeId);
            if (!StringUtil.isEmpty(insJson)) {
                HeartbeatRsp data = new HeartbeatRsp();
                data.setIns(GsonUtil.fromJson(insJson, Ins.class));
                dataRsp.setData(data);
            }
        }

        // update node_rt
        NodeRt nodeRt = new NodeRt();
        nodeRt.setOnlineTime(System.currentTimeMillis());
        nodeRt.setStatus(NodeConst.STATUS_NORMAIL);

        Plugin.sendToNode(new TextMsg().setType(MsgType.NODE_RT_UPDATE).setValue(GsonUtil.toJson(nodeRt)), nodeId);

        return dataRsp;
    }

    @Override
    protected Heartbeat decodeDataReq(IoContext context) {
        return context.dataCodec().decode(context.request().data(), Heartbeat.class);
    }

}
