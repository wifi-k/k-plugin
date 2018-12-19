package tbcloud.node.api.handler;

import tbcloud.common.model.IpInfo;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.ConfField;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeRt;
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
        int tokenExpired = tokenExpired(); // hours
        setToRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_TOKEN_ + token, nodeId, tokenExpired * ApiConst.REDIS_EXPIRED_1H);

        // TODO rm old token

        // update node_rt TODO async
        NodeRt rt = new NodeRt();
        rt.setNodeId(nodeId);
        rt.setStatus(NodeConst.STATUS_NORMAIL);
        rt.setOnlineTime(System.currentTimeMillis());
        rt.setToken(token);
        NodeDao.updateNodeRt(rt);

        // update node_info TODO async
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
        if (!ip.equals(nodeInfo.getIp()))
            nodeInfo.setIp(ip);
        NodeDao.updateNodeInfo(info);

        // insert ip_info TODO aysnc
        if (!ip.equals(nodeInfo.getIp())) {
            IpInfo ipInfo = CommonDao.selectIpInfo(ip);
            if (ipInfo == null) {
                ipInfo = new IpInfo();
                ipInfo.setIp(ip);
                CommonDao.insertIpInfo(ipInfo);
            }
        }

        dataRsp.setCode(ApiCode.SUCC);
        NodeAuthRsp data = new NodeAuthRsp();
        data.setToken(token);
        data.setInsHost(insHost());
        data.setTokenExpired(tokenExpired - 1);// before 1 hour
        dataRsp.setData(data);

        return dataRsp;
    }

    @Override
    protected NodeAuth decodeDataReq(IoContext context) {
        //LOG.info("data {}", new String(context.request().data().array(), PacketConst.CHARSET));
        return context.dataCodec().decode(context.request().data(), NodeAuth.class);
    }

    // 指令服务器地址
    public String insHost() {
        return plugin().getConfig(ConfField.NODE_API_INS_HOST, "127.0.0.1:9019");
    }

    public int tokenExpired() {
        int tokenExpired = Integer.parseInt(plugin().getConfig(ConfField.NODE_API_TOKEN_EXPIRED, "24"));
        return tokenExpired;
    }

}
