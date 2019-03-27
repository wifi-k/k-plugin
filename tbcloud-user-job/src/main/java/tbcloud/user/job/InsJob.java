package tbcloud.user.job;

import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeIns;
import tbcloud.node.protocol.data.ins.Ins;

/**
 * @author dzh
 * @date 2019-03-27 12:07
 */
public abstract class InsJob extends UserJob {

    protected void saveThenSend(String nodeId, Ins ins) {
        saveIns(nodeId, ins);
        sendIns(nodeId, ins);
    }

    protected void saveIns(String nodeId, Ins ins) {
        NodeIns nodeIns = new NodeIns();
        nodeIns.setId(ins.getId());
        nodeIns.setNodeId(nodeId);
        nodeIns.setIns(ins.getIns());
        nodeIns.setStatus(NodeConst.INS_STATUS_SEND);
        nodeIns.setVal(ins.getVal());
        nodeIns.setSendTime(System.currentTimeMillis());
        nodeIns.setRetry(0);
        NodeDao.insertNodeIns(nodeIns);
    }

    protected boolean sendIns(String nodeId, Ins ins) { //TODO mq 在一个地方统一发送,一个节点的发送保持有序
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
