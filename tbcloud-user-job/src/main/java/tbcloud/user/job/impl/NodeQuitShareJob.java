package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.AppEnum;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.*;
import tbcloud.node.protocol.data.ins.HttpProxy;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.user.job.UserJob;

/**
 * @author dzh
 * @date 2018-12-13 19:03
 */
public class NodeQuitShareJob extends UserJob {
    @Override
    public int msgType() {
        return MsgType.NODE_QUIT_SHARE;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        NodeInfo nodeInfo = null;
        if (val instanceof String) { // maybe from mq
            nodeInfo = GsonUtil.fromJson((String) val, NodeInfo.class);
        } else if (val instanceof NodeInfo) {
            nodeInfo = (NodeInfo) val;
        } else {
            return;
        }

        String nodeId = nodeInfo.getNodeId();
        // set node unshare
        NodeDao.updateNodeInfo(nodeInfo);

        // remove share app
        NodeApp nodeApp = new NodeApp();
        nodeApp.setIsDelete(ApiConst.IS_DELETE_Y);
        NodeAppExample example = new NodeAppExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andAppIdEqualTo(AppEnum.HTTP_PROXY.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        NodeDao.updateNodeApp(nodeApp);

        // insert node_ins
        String insHost = plugin().getConfig(ApiConst.NODE_API_INS_HOST);
        HttpProxy insHttpProxy = new HttpProxy();
        insHttpProxy.setOp(HttpProxy.OP_DISABLE);

        Ins ins = new Ins();
        ins.setId(IDUtil.genInsId(nodeId, Ins.INS_HTTPPROXY));
        ins.setIns(Ins.INS_HTTPPROXY);
        ins.setVal(GsonUtil.toJson(insHttpProxy));

        NodeIns nodeIns = new NodeIns();
        nodeIns.setId(ins.getId());
        nodeIns.setNodeId(nodeId);
        nodeIns.setIns(ins.getIns());
        nodeIns.setStatus(NodeConst.INS_STATUS_SEND);
        nodeIns.setVal(ins.getVal());
        nodeIns.setSendTime(System.currentTimeMillis());
        nodeIns.setRetry(0);
        NodeDao.insertNodeIns(nodeIns);

        // send ins
        addToRedis(nodeId, ins);
    }

    boolean addToRedis(String nodeId, Ins ins) { //TODO mq 在一个地方统一发送,一个节点的发送保持有序
        if (StringUtil.isEmpty(nodeId) || ins == null) {
            LOG.error("nodeId is nil");
            return false;
        }
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(ApiConst.REDIS_ID_NODE)) { //TODO 并行指令、指令预测
            long count = jedis.scard(ApiConst.REDIS_KEY_NODE_INS_ + nodeId);
            if (count == 0) {
                jedis.sadd(ApiConst.REDIS_KEY_NODE_INS_ + nodeId, GsonUtil.toJson(ins));
                LOG.info("node {}, add ins {} {} {}", nodeId, ins.getId(), ins.getIns(), ins.getVal());
                return true;
            }
        }
        return false;
    }

}
