package tbcloud.user.api;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.annotation.Plugin;
import jframe.ext.plugin.KafkaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.elastic.model.client.TbcloudESClient;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MsgMeta;

import java.io.IOException;

/**
 * @author dzh
 * @date 2018-11-08 19:58
 */
@Plugin(startOrder = -1)
public class UserApiPlugin extends KafkaPlugin {

    static Logger LOG = LoggerFactory.getLogger(UserApiPlugin.class);

    private ApiHttpServer httpServer;

    private TbcloudESClient elastic;

    public void start() throws PluginException {
        super.start();

        try {
            elastic = new TbcloudESClient(getConfig("file.es"));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        httpServer = new ApiHttpServer(this);
        httpServer.startHttpServer();
    }

    public TbcloudESClient elastic() {
        return elastic;
    }

    public void stop() throws PluginException {
        super.stop();
        httpServer.stopHttpServer();
        if (elastic != null) elastic.close();
    }

    // 开启调试模式 用于测试
    public boolean isDebug() {
        return "true".equalsIgnoreCase(getConfig(ApiConst.DEBUG_ENABLED, "false").trim());
    }

    public String envName() {
        return getConfig(ApiConst.ENV_NAME, ApiConst.ENV_ONLINE).trim();
    }

    /**
     * send msg to topic `tbcUser`
     *
     * @param msg
     * @param userId
     */
    public void sendToUser(Msg<String> msg, Long userId) {
        if (userId == null)
            sendToUser(msg);
        else
            send(msg, MsgMeta.Topic_User, String.valueOf(userId));
    }

    /**
     * send msg to topic `tbcUser`
     *
     * @param msg
     */
    public void sendToUser(Msg<String> msg) {
        send(msg, MsgMeta.Topic_User);
    }

}
