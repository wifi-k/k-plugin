package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeIns;
import tbcloud.node.model.NodeInsExample;

/**
 * 把失效的指令设置为发送失败
 *
 * @author dzh
 * @date 2018-12-13 19:32
 */
public class NodeInsFailed extends MutexTimer {

    @Override
    protected int doTimer() {
        NodeInsExample example = new NodeInsExample();
        example.createCriteria().andStatusEqualTo(NodeConst.INS_STATUS_SEND).andRetryGreaterThanOrEqualTo(NodeConst.INS_MAX_RETRY)
                .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);

        NodeIns failedIns = new NodeIns();
        failedIns.setStatus(NodeConst.INS_STATUS_FAIL);
        int count = NodeDao.updateNodeInsSelective(failedIns, example);

        // TODO alert

        return count;
    }

    @Override
    protected String path() {
        return "/node/ins/failed";
    }

    @Override
    protected long delayMs() {
        return 30 * 60 * 1000;
    }

    @Override
    protected String name() {
        return "NodeInsFailed";
    }
}
