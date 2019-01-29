package tbcloud.open.httpproxy;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.annotation.Plugin;
import jframe.ext.plugin.KafkaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.msg.MsgMeta;
import tbcloud.lib.api.util.StringUtil;

import java.lang.management.ManagementFactory;
import java.util.Calendar;

/**
 * @author dzh
 * @date 2018-11-26 01:13
 */
@Plugin(startOrder = -1)
public class OpenHttpProxyPlugin extends KafkaPlugin {

    static final Logger LOG = LoggerFactory.getLogger(OpenHttpProxyPlugin.class);

    private HttpProxyServer httpProxyServer;

    private int serverId; //TODO global unique id

    public void start() throws PluginException {
        super.start();

        serverId = Math.abs(ManagementFactory.getRuntimeMXBean().getName().hashCode() + Calendar.getInstance().hashCode());
        LOG.info("serverId {}", serverId);

        httpProxyServer = new HttpProxyServer(this);
        httpProxyServer.start();
    }

    public void stop() throws PluginException {
        super.stop();

        if (httpProxyServer != null) {
            try {
                httpProxyServer.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public int serverId() {
        return serverId;
    }

    public void sendToHttpProxy(Msg<String> msg, String nodeId) {
        if (StringUtil.isEmpty(nodeId))
            sendToHttpProxy(msg);
        else
            send(msg, MsgMeta.Topic_HttpProxy, nodeId);
    }

    public void sendToHttpProxy(Msg<String> msg) {
        send(msg, MsgMeta.Topic_HttpProxy);
    }
}
