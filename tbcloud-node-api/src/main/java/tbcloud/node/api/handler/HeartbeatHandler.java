package tbcloud.node.api.handler;

import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeRt;
import tbcloud.node.model.NodeRtExample;
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

        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(ApiConst.REDIS_ID_NODE)) {
            String insJson = jedis.spop(ApiConst.REDIS_KEY_NODE_INS_ + nodeId);
            if (!StringUtil.isEmpty(insJson)) {
                HeartbeatRsp data = new HeartbeatRsp();
                data.setIns(GsonUtil.fromJson(insJson, Ins.class));
                dataRsp.setData(data);
            }
        }

        // update node_rt TODO fix
        NodeRt nodeRt = new NodeRt();
        nodeRt.setOnlineTime(System.currentTimeMillis());  //TODO offline
        nodeRt.setStatus(NodeConst.STATUS_NORMAIL);

        NodeRtExample example = new NodeRtExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andStatusEqualTo(NodeConst.STATUS_OFFLINE);
        NodeDao.updateNodeRtSelective(nodeRt, example);

        return dataRsp;
    }

    @Override
    protected Heartbeat decodeDataReq(IoContext context) {
        return context.dataCodec().decode(context.request().data(), Heartbeat.class);
    }

}
