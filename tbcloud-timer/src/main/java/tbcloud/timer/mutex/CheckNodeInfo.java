package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeInfoExample;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dzh
 * @date 2019-03-25 14:13
 */
public class CheckNodeInfo extends MutexTimer {
    @Override
    protected int doTimer() {
        NodeInfoExample example = new NodeInfoExample();
        example.createCriteria().andInviteCodeIsNull().andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("create_time 1000");


        List<NodeInfo> list = NodeDao.selectNodeInfo(example);
        if (list != null && !list.isEmpty()) {
            List<NodeInfo> updated = new ArrayList<>(list.size());

            list.forEach(node -> {
                NodeInfo up = new NodeInfo();
                up.setNodeId(node.getNodeId());
                up.setInviteCode(IDUtil.genNodeInviteCode());
                updated.add(up);
            });

            NodeDao.batchUpdateNodeInfo(updated);
        }

        return list.size();
    }

    @Override
    protected String path() {
        return "/node/info/check";
    }

    @Override
    protected long delayMs() {
        return 30 * 1000;
    }

}
