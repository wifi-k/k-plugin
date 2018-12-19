package tbcloud.share.httpproxy;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSenderRecver;
import jframe.core.plugin.annotation.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Calendar;

/**
 * @author dzh
 * @date 2018-11-26 01:13
 */
@Plugin(startOrder = -1)
public class ShareHttpProxyPlugin extends PluginSenderRecver {

    static Logger LOG = LoggerFactory.getLogger(ShareHttpProxyPlugin.class);

    private HttpProxyServer httpProxyServer;

    private int serverId; //TODO global unique id redis or zk

    public void start() throws PluginException {
        super.start();

        serverId = Math.abs(ManagementFactory.getRuntimeMXBean().getName().hashCode() + Calendar.getInstance().hashCode());

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

    @Override
    protected void doRecvMsg(Msg<?> msg) {

    }

    @Override
    protected boolean canRecvMsg(Msg<?> msg) {
        return false;
    }
}
