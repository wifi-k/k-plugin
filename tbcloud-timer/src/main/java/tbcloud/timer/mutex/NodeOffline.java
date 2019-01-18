package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeRt;
import tbcloud.node.model.NodeRtExample;
import tbcloud.node.protocol.ProtocolConst;

/**
 * 在线节点但超过5次心跳没有更新online_time,设置为离线
 * <p>
 * 消耗性能 想其他方式做 TODO
 *
 * @author dzh
 * @date 2018-12-13 19:57
 */
public class NodeOffline extends MutexTimer {

    @Override
    protected int doTimer() {
        long ts = System.currentTimeMillis();
        NodeRtExample example = new NodeRtExample();
        example.createCriteria().andStatusGreaterThan(NodeConst.STATUS_OFFLINE)
                .andOnlineTimeLessThan(ts - 5 * ProtocolConst.HEARTBEAT_TICK).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);

        NodeRt offline = new NodeRt();
        offline.setStatus(NodeConst.STATUS_OFFLINE);
        offline.setOfflineTime(ts);

        int count = NodeDao.updateNodeRtSelective(offline, example);
        LOG.info("node offline {}", count);
        // TODO alert user？

        return count;
    }

    @Override
    protected String path() {
        return "/node/info/offline";
    }

    @Override
    protected long delayMs() {
        return 3600 * 1000;
    }

    @Override
    protected String name() {
        return "NodeOffline";
    }
}
