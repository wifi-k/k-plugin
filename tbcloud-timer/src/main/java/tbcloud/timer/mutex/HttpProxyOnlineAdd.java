package tbcloud.timer.mutex;

import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.httpproxy.model.HttpProxyOnlineExample;
import tbcloud.lib.api.ApiConst;
import tbcloud.node.protocol.ProtocolConst;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 这里是一个容错机制。查询在线的httpproxy节点，加入到redis的集合
 * <p>
 * 集合的维护通过JoinHttpProxyJob\QuitHttpProxyJob消息更新
 *
 * @author dzh
 * @date 2018-12-20 21:00
 */
public class HttpProxyOnlineAdd extends MutexTimer {
    @Override
    protected int doTimer() {
        HttpProxyOnlineExample example = new HttpProxyOnlineExample();
        example.createCriteria().andStatusEqualTo(ApiConst.IS_ONLINE).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("online_time limit 1000");
        List<HttpProxyOnline> onlineList = HttpProxyDao.selectHttpProxyOnline(example);
        int size = onlineList.size();
        if (size < 1) {
            LOG.warn("HttpProxyOnline {}", size);
            // TODO send alert
            return size;
        }

        // TODO 依据节点的健康状态加入好的节点

        HttpProxyOnline online = null;
        String[] randomOnline = null;
        if (size < 1000) {
            randomOnline = new String[Math.min(size, 100)];
            for (int i = 0; i < randomOnline.length; ++i) {
                randomOnline[i] = onlineList.get(i).getNodeId(); //取最近的N个
            }
        } else {
            randomOnline = new String[100];
            for (int i = 0; i < randomOnline.length; ++i) {
                online = onlineList.get(ThreadLocalRandom.current().nextInt(size)); //随机加入100个
                randomOnline[i] = online.getNodeId();
            }
        }
        saddToRedis(ApiConst.REDIS_ID_HTTPPROXY, ApiConst.REDIS_KEY_NODE_HTTPPROXY_ALL, randomOnline);

        return size;
    }

    @Override
    protected String path() {
        return "/httpproxy/online/all";
    }

    @Override
    protected long delayMs() {
        return ProtocolConst.HEARTBEAT_TICK;
    }

    @Override
    protected String name() {
        return "HttpProxyOnlineAdd";
    }
}
