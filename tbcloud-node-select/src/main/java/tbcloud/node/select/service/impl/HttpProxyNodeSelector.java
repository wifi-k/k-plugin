package tbcloud.node.select.service.impl;

import tbcloud.lib.api.ApiConst;

import java.util.List;

/**
 * @author dzh
 * @date 2018-12-20 19:38
 */
class HttpProxyNodeSelector extends NodeSelector {

    // TODO
    public List<String> randomOnline(int count) {
        return srandmemberFromRedis(ApiConst.REDIS_ID_HTTPPROXY, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ALL, count);
    }
}
