package tbcloud.node.api;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.annotation.Plugin;
import jframe.ext.plugin.KafkaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.ConfField;
import tbcloud.lib.api.msg.MsgMeta;
import tbcloud.lib.api.util.StringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author dzh
 * @date 2018-11-11 17:55
 */
@Plugin(startOrder = -1)
public class NodeApiPlugin extends KafkaPlugin {

    static Logger LOG = LoggerFactory.getLogger(NodeApiPlugin.class);

    private NodeApiServerNio nodeServer;

    public void start() throws PluginException {
        super.start();

        try {
            startNodeServer();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void startNodeServer() throws IOException {
        String ip = getConfig(ConfField.NDOE_API_HOST, "0.0.0.0");
        String port = getConfig(ConfField.NODE_API_PORT, "9019");
        InetSocketAddress addr = new InetSocketAddress(ip, Integer.parseInt(port));
        LOG.info("NodeApiServer listen on {}", addr);

        nodeServer = new NodeApiServerNio(addr, isAuth());
        nodeServer.start();
    }

    public void stop() throws PluginException {
        super.stop();

        stopNodeServer();
    }

    private void stopNodeServer() {
        try {
            nodeServer.close();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    // 开启调试模式 用于测试
    public boolean isDebug() {
        return "true".equalsIgnoreCase(getConfig(ConfField.DEBUG_ENABLED, "false").trim());
    }

    public String envName() {
        return getConfig(ConfField.ENV_NAME, ApiConst.ENV_ONLINE).trim();
    }

    // 开启认证节点功能
    public boolean isAuth() {
        return "true".equalsIgnoreCase(getConfig(ConfField.NODE_API_AUTH_ENABLED, "true").trim());
    }

    public void sendToNode(Msg<String> msg, String nodeId) {
        if (StringUtil.isEmpty(nodeId))
            sendToNode(msg);
        else
            send(msg, MsgMeta.Topic_Node, nodeId);
    }

    public void sendToNode(Msg<String> msg) {
        send(msg, MsgMeta.Topic_Node);
    }

}
