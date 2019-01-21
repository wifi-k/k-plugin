package tbcloud.node.httpproxy;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.annotation.Plugin;
import jframe.ext.plugin.KafkaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.msg.MsgMeta;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.httpproxy.dispatch.HttpProxyDispatch;

import java.lang.management.ManagementFactory;
import java.util.Calendar;

/**
 * @author dzh
 * @date 2018-11-26 01:09
 */
@Plugin(startOrder = -1)
public class NodeHttpProxyPlugin extends KafkaPlugin {

    static Logger LOG = LoggerFactory.getLogger(NodeHttpProxyPlugin.class);

    private int serverId; //TODO global unique id

    private NodeHttpServer httpServer;
    private NodeTcpServer tcpServer;

    private HttpProxyDispatch dispatch = new HttpProxyDispatch();

    public void start() throws PluginException {
        super.start();

        serverId = Math.abs(ManagementFactory.getRuntimeMXBean().getName().hashCode() + Calendar.getInstance().hashCode());
        LOG.info("serverId {}", serverId);

        httpServer = new NodeHttpServer(this);
        httpServer.start();
        tcpServer = new NodeTcpServer(this);
        tcpServer.start();
    }

    public int serverId() {
        return serverId;
    }

    public HttpProxyDispatch dispatch() {
        return this.dispatch;
    }

    public void stop() throws PluginException {
        super.stop();

        // TODO 防止数据丢失
        if (httpServer != null) {
            try {
                httpServer.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (tcpServer != null) {
            try {
                tcpServer.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (dispatch != null) {
            try {
                dispatch.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public NodeHttpServer getHttpServer() {
        return httpServer;
    }

    public void sendToNode(Msg<String> msg, String nodeId) {
        if (StringUtil.isEmpty(nodeId))
            sendToNode(msg);
        else
            send(msg, MsgMeta.Topic_HttpProxy, nodeId);
    }

    public void sendToNode(Msg<String> msg) {
        send(msg, MsgMeta.Topic_HttpProxy);
    }

}