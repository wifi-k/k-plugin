package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeIns;
import tbcloud.node.model.NodeInsExample;
import tbcloud.node.protocol.ProtocolConst;
import tbcloud.node.protocol.data.ins.Ins;

import java.util.LinkedList;
import java.util.List;

/**
 * 在达到最大重试次数前重发指令
 *
 * @author dzh
 * @date 2018-12-13 10:30
 */
public class NodeInsRetry extends MutexTimer {

    @Override
    protected int doTimer() {
        NodeInsExample example = new NodeInsExample();
        long ts = System.currentTimeMillis();
        // TODO 加入到延迟队列
        // TODO status设置为超时
        // 重发超过1个心跳没有收到回复的指令
        example.createCriteria().andStatusEqualTo(NodeConst.INS_STATUS_SEND).andSendTimeLessThan(ts - ProtocolConst.HEARTBEAT_TICK)
                //.andCreateTimeGreaterThan(ts - 10 * NodeConst.HEARTBEAT_TICK)
                .andRetryLessThan(NodeConst.INS_MAX_RETRY)
                .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("create_time limit 1000"); // 先创建的优先 TODO
        List<NodeIns> list = NodeDao.selectNodeIns(example);

        List<NodeIns> retryList = new LinkedList<>();
        list.forEach(r -> { // TODO async to send msg with job
            NodeIns updated = new NodeIns();
            updated.setId(r.getId());
            updated.setRetry(r.getRetry() + 1);
            retryList.add(updated);

            Ins ins = new Ins();
            ins.setId(r.getId());
            ins.setIns(r.getIns());
            ins.setVal(r.getVal());
            try {
                if (addToRedis(r.getNodeId(), ins)) {
                    updated.setSendTime(System.currentTimeMillis());
                    LOG.info("node {}, retry {} ins {} {} {}", r.getNodeId(), updated.getRetry(), ins.getId(), ins.getIns(), ins.getVal());
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
        NodeDao.batchUpdateNodeIns(retryList);

        return list.size();
    }


    boolean addToRedis(String nodeId, Ins ins) {
        if (StringUtil.isEmpty(nodeId) || ins == null) {
            LOG.warn("nodeId is nil");
            return false;
        }
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(ApiConst.REDIS_ID_NODE)) {
            long count = jedis.scard(ApiConst.REDIS_KEY_NODE_INS_ + nodeId);
            if (count == 0) { //目前保证指令有序发送
                jedis.sadd(ApiConst.REDIS_KEY_NODE_INS_ + nodeId, GsonUtil.toJson(ins));
                return true;
            }
        }
        return false;
    }

    @Override
    protected String path() {
        return "/node/ins/retry";
    }

    @Override
    protected long delayMs() {
        return ProtocolConst.HEARTBEAT_TICK;
    }

    @Override
    protected String name() {
        return "NodeInsRetry";
    }
}
