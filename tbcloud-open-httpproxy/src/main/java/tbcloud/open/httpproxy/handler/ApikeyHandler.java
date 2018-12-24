package tbcloud.open.httpproxy.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.user.model.UserInfo;

/**
 * @author dzh
 * @date 2018-12-06 20:10
 */
@ChannelHandler.Sharable
public class ApikeyHandler extends AbstractInboundHandler {

    static Logger LOG = LoggerFactory.getLogger(ApikeyHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            // check apikey
            String apikey = ((HttpRequest) msg).headers().get(ApiConst.API_APIKEY);
            String apiVersion = ((HttpRequest) msg).headers().get(ApiConst.API_VERSION);
            long userId = IDUtil.readUserIdFromApikey(apikey);
            if (userId < 1) {
                writeError(ctx, false, null, newResult(ApiCode.INVALID_APIKEY, "invalid apikey " + apikey));
                return;
            }

            LOG.info("req {} {} {} {} ", userId, apiVersion, ((HttpRequest) msg).uri(), ((HttpRequest) msg).method());

            UserInfo userInfo = UserDao.selectUserInfo(userId);
            if (userInfo == null) {
                writeError(ctx, false, null, newResult(ApiCode.INVALID_APIKEY, "invalid apikey " + apikey));
                return;
            }

            // TODO 频率限制

        }
        ctx.fireChannelRead(msg);
    }

}