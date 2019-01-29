package tbcloud.open.httpproxy.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.open.httpproxy.handler.util.HttpProxyRecordUtil;
import tbcloud.user.model.UserInfo;

/**
 * @author dzh
 * @date 2018-12-06 20:10
 */
public class ApikeyHandler extends AbstractInboundHandler {

    static Logger LOG = LoggerFactory.getLogger(ApikeyHandler.class);

    boolean isValid = true;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            isValid = true;
            // check apikey
            String apikey = ((HttpRequest) msg).headers().get(ApiConst.API_APIKEY);
            String apiVersion = ((HttpRequest) msg).headers().get(ApiConst.API_VERSION);
            long userId = IDUtil.readUserIdFromApikey(apikey);
            String uri = ((HttpRequest) msg).uri();
            LOG.info("req {} {} {} {} ", userId, apiVersion, uri, ((HttpRequest) msg).method());

            if ("/api/test/ping".equals(uri)) { // health checking
                isValid = false;
                writeResponse(ctx, false, null, newResult(ApiCode.SUCC, "ping"), null);
                return;
            }

            if (userId < 1) {
                isValid = false;
                writeResponse(ctx, false, null, newResult(ApiCode.INVALID_APIKEY, "invalid apikey " + apikey),
                        HttpProxyRecordUtil.toRecord(((HttpRequest) msg), HttpProxyConst.PROXY_STATUS_FAIL, null));
                return;
            }

            UserInfo userInfo = UserDao.selectUserInfo(userId);
            if (userInfo == null) {
                isValid = false;
                writeResponse(ctx, false, null, newResult(ApiCode.INVALID_APIKEY, "invalid apikey " + apikey),
                        HttpProxyRecordUtil.toRecord(((HttpRequest) msg), HttpProxyConst.PROXY_STATUS_FAIL, null));
                return;
            }

            // TODO 频率限制

        }
        if (isValid) ctx.fireChannelRead(msg);
    }


}
