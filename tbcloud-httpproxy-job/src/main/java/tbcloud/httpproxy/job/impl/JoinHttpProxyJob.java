package tbcloud.httpproxy.job.impl;

import jframe.core.msg.Msg;
import tbcloud.httpproxy.job.HttpProxyJob;
import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;

/**
 * @author dzh
 * @date 2018-12-20 21:51
 */
public class JoinHttpProxyJob extends HttpProxyJob {
    @Override
    public int msgType() {
        return MsgType.NODE_JOIN_HTTPPROXY;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        HttpProxyOnline online = null;
        if (val instanceof String) { // maybe from mq in the future
            online = GsonUtil.fromJson((String) val, HttpProxyOnline.class);
        } else if (val instanceof HttpProxyOnline) {
            online = (HttpProxyOnline) val;
        }

        if (online == null) { // TODO why
            return;
        }

        String nodeId = online.getNodeId();
        HttpProxyOnline oldOnline = HttpProxyDao.selectHttpProxyOnline(nodeId);
        if (oldOnline == null) {
            HttpProxyDao.insertHttpProxyOnline(online);
        } else {
            HttpProxyDao.updateHttpProxyOnline(online);
        }

        saddFromRedis(ApiConst.REDIS_ID_HTTPPROXY, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ALL, nodeId);
    }

    @Override
    protected String id() {
        return "JoinHttpProxyJob";
    }
}
