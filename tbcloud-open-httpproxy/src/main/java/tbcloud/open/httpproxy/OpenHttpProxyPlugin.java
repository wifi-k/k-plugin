package tbcloud.open.httpproxy;

import jframe.core.plugin.DefPlugin;
import jframe.core.plugin.PluginException;
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
public class OpenHttpProxyPlugin extends DefPlugin {

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
}
