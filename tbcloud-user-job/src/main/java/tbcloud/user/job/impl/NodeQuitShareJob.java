package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.AppEnum;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.node.model.NodeApp;
import tbcloud.node.model.NodeAppExample;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.protocol.data.ins.HttpProxy;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.user.job.InsJob;

/**
 * @author dzh
 * @date 2018-12-13 19:03
 */
public class NodeQuitShareJob extends InsJob {
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

        saveThenSend(nodeId, ins);
    }

}
