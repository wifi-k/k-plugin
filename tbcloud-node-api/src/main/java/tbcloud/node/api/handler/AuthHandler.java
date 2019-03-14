package tbcloud.node.api.handler;

import jframe.core.msg.TextMsg;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.ConfField;
import tbcloud.lib.api.msg.MsgMeta;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeRt;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.data.DataRsp;
import tbcloud.node.protocol.data.NodeAuth;
import tbcloud.node.protocol.data.rsp.NodeAuthRsp;

/**
 * @author dzh
 * @date 2018-11-28 19:09
 */
public class AuthHandler extends DataHandler<NodeAuth> {

    public AuthHandler(IoContext context) {
        super(context);
    }

    @Override
    protected DataRsp<NodeAuthRsp> handle(NodeAuth dataReq) {
        String nodeId = dataReq.getNodeId();
        DataRsp<NodeAuthRsp> dataRsp = new DataRsp<>();

        // node must be bound
        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null || nodeInfo.getIsBind() == NodeConst.IS_UNBIND) {
            dataRsp.setCode(ApiCode.NODE_IS_UNBIND);
            return dataRsp;
        }

        String token = IDUtil.genNodeToken(nodeId);
        int tokenExpired = tokenExpired(); // seconds
        setToRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_TOKEN_ + token, nodeId, tokenExpired + 600);

        // update node_rt
        NodeRt rt = new NodeRt();
        rt.setNodeId(nodeId);
        rt.setStatus(NodeConst.STATUS_NORMAIL);
        rt.setOnlineTime(System.currentTimeMillis());
        rt.setToken(token);
        Plugin.sendToNode(new TextMsg().setType(MsgType.NODE_RT_UPDATE).setValue(GsonUtil.toJson(rt)), nodeId);
        //NodeDao.updateNodeRt(rt);

        // update node_info
        NodeInfo info = new NodeInfo();
        info.setNodeId(nodeId);
        info.setPartner(dataReq.getParnter());
        info.setManufactory(dataReq.getManufactory());
        info.setModel(dataReq.getModel());
        info.setFirmware(dataReq.getFirmware());
        info.setMemory(dataReq.getMemory());
        info.setDisk(dataReq.getDisk());
        info.setUpstream(dataReq.getUpstream());
        info.setDownstream(dataReq.getDownstream());
        String ip = context().remote().getAddress().getHostAddress();
        if (!ip.equals(nodeInfo.getIp())) info.setIp(ip);
        Plugin.sendToNode(new TextMsg().setType(MsgType.NODE_INFO_UPDATE)
                .setValue(GsonUtil.toJson(info)).setMeta(MsgMeta.Node_Ssid_List, dataReq.getSsid()), nodeId);
//        NodeDao.updateNodeInfo(info);

        // upsert node_wifi


        // insert ip_info
//        if (!ip.equals(nodeInfo.getIp())) {
//            IpInfo ipInfo = CommonDao.selectIpInfo(ip);
//            if (ipInfo == null) {
//                ipInfo = new IpInfo();
//                ipInfo.setIp(ip);
//                CommonDao.insertIpInfo(ipInfo);
//            }
//        }

        dataRsp.setCode(ApiCode.SUCC);
        NodeAuthRsp data = new NodeAuthRsp();
        data.setToken(token);
        data.setInsHost(insHost());
        data.setTickTime(tickTime());
        data.setTokenExpired(tokenExpired);

        dataRsp.setData(data);

        return dataRsp;
    }

    @Override
    protected NodeAuth decodeDataReq(IoContext context) {
        LOG.info("data {}", new String(context.request().data().array(), PacketConst.UTF_8));
        return context.dataCodec().decode(context.request().data(), NodeAuth.class);
    }

    // 指令服务器地址
    public String insHost() {
        return plugin().getConfig(ConfField.NODE_API_INS_HOST, "127.0.0.1:9019");
    }

    public int tokenExpired() {
        int tokenExpired = Integer.parseInt(plugin().getConfig(ConfField.NODE_API_TOKEN_EXPIRED, "86400")); // second,24h
        return tokenExpired;
    }

    public int tickTime() {
        int tickTime = Integer.parseInt(plugin().getConfig(ConfField.NODE_API_TICK_TIME, "3")); // second
        return tickTime;
    }

}
