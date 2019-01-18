package tbcloud.node.job.impl;

import jframe.core.msg.Msg;
import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.job.NodeJob;

/**
 * @author dzh
 * @date 2018-12-20 21:51
 */
public class QuitHttpProxyJob extends NodeJob {
    @Override
    public int msgType() {
        return MsgType.NODE_QUIT_HTTPPROXY;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        HttpProxyOnline offline = null;
        if (val instanceof String) { // maybe from mq in the future
            offline = GsonUtil.fromJson((String) val, HttpProxyOnline.class);
        } else if (val instanceof HttpProxyOnline) {
            offline = (HttpProxyOnline) val;
        }

        if (offline == null) { //TODO why
            return;
        }
        HttpProxyDao.updateHttpProxyOnline(offline);

        // remove nodeId from redis online set
        sremFromRedis(ApiConst.REDIS_ID_NODE, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ALL, offline.getNodeId());
    }

    @Override
    protected String id() {
        return "QuitHttpProxyJob";
    }
}
