package tbcloud.share.httpproxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.user.model.UserInfo;

/**
 * @author dzh
 * @date 2018-12-06 20:10
 */
public class ApikeyHandler extends AbstractInboundHandler {

    static Logger LOG = LoggerFactory.getLogger(ApikeyHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            // check apikey
            String apikey = ((HttpRequest) msg).headers().get(ApiConst.API_APIKEY);
            String apiVersion = ((HttpRequest) msg).headers().get(ApiConst.API_VERSION);

            LOG.info("apikey {} {} {} {} ", apikey, apiVersion, ((HttpRequest) msg).uri(), ((HttpRequest) msg).method());

            long userId = IDUtil.readUserIdFromApikey(apikey);
            UserInfo userInfo = UserDao.selectUserInfo(userId);


            if (userInfo == null) {

            }

            // insert record
        }
        ctx.fireChannelRead(msg);
    }


}
