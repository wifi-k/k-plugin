package tbcloud.node.httpproxy.dispatch;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO 监控节点的调用情况
 * TODO MAP的内存占用问题，比如10k代理
 *
 * @author dzh
 * @date 2018-12-16 17:06
 */
public class HttpProxyDispatch implements AutoCloseable {

    static Logger LOG = LoggerFactory.getLogger(HttpProxyDispatch.class);

    private ConcurrentMap<String, DispatchNode> dispatchNode = new ConcurrentHashMap<>();

    @Override
    public void close() throws Exception {
        dispatchNode.forEach((n, g) -> {
            try {
                g.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    public void attachNode(String nodeId, ChannelHandlerContext context) {
        DispatchNode prevGroup = dispatchNode.put(nodeId, new DispatchNode(context));
        if (prevGroup != null) {
            try {
                prevGroup.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        LOG.info("attachNode {}", nodeId);
    }

    public void detachNode(String nodeId) {
        DispatchNode group = dispatchNode.remove(nodeId);
        if (group != null) {
            try {
                group.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        LOG.info("detachNode {}", nodeId);
    }

    public DispatchNode findNode(String nodeId) {
        return dispatchNode.get(nodeId);
    }

    @Override
    public String toString() {
        return dispatchNode.toString();
    }

}
