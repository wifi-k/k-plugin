package tbcloud.user.api;

import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSender;
import jframe.core.plugin.annotation.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dzh
 * @date 2018-11-08 19:58
 */
@Plugin(startOrder = -1)
public class UserApiPlugin extends PluginSender {


    static Logger LOG = LoggerFactory.getLogger(UserApiPlugin.class);

    private ApiHttpServer httpServer;


    public void start() throws PluginException {
        super.start();

        httpServer = new ApiHttpServer(this);
        httpServer.startHttpServer();

    }

    public void stop() throws PluginException {
        super.stop();
        httpServer.stopHttpServer();
    }

}
