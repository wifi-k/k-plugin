package tbcloud.share.httpproxy;

import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSender;
import jframe.core.plugin.annotation.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author dzh
 * @date 2018-11-26 01:13
 */
@Plugin(startOrder = -1)
public class ShareHttpProxyPlugin extends PluginSender {

    static Logger LOG = LoggerFactory.getLogger(ShareHttpProxyPlugin.class);

    private HttpProxyServer httpProxyServer;

    public void start() throws PluginException {
        super.start();

        try {
            startHttpProxyServer();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void startHttpProxyServer() throws IOException {
        httpProxyServer = new HttpProxyServer(this);
        httpProxyServer.start();

    }

    public void stop() throws PluginException {
        super.stop();

        if (httpProxyServer != null) {
            try {
                httpProxyServer.close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

}
