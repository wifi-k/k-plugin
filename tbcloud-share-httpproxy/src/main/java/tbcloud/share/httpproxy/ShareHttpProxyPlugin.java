package tbcloud.share.httpproxy;

import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSender;
import jframe.core.plugin.annotation.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ConfField;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author dzh
 * @date 2018-11-26 01:13
 */
@Plugin(startOrder = -1)
public class ShareHttpProxyPlugin extends PluginSender {

    static Logger LOG = LoggerFactory.getLogger(ShareHttpProxyPlugin.class);

    public void start() throws PluginException {
        super.start();

        try {
            startNodeServer();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void startNodeServer() throws IOException {
        String ip = getConfig(ConfField.NDOE_API_IP, "0.0.0.0");
        String port = getConfig(ConfField.NODE_API_PORT, "8108");
        InetSocketAddress addr = new InetSocketAddress(ip, Integer.parseInt(port));
        LOG.info("ShareHttpProxyApi listen on {}", addr);

    }

    public void stop() throws PluginException {
        super.stop();
    }

}
