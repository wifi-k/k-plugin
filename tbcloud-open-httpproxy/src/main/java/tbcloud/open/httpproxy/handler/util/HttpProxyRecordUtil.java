package tbcloud.open.httpproxy.handler.util;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.open.httpproxy.OpenHttpProxyPlugin;

import java.net.URI;

/**
 * @author dzh
 * @date 2019-01-29 12:06
 */
@Injector
public class HttpProxyRecordUtil {

    static final Logger LOG = LoggerFactory.getLogger(HttpProxyRecordUtil.class);

    @InjectPlugin
    protected static OpenHttpProxyPlugin Plugin;

    public static HttpProxyRecord toRecord(HttpRequest msg, byte proxyStatus, String nodeId) {
        HttpProxyRecord record = new HttpProxyRecord();

        long userId = IDUtil.readUserIdFromApikey(msg.headers().get(ApiConst.API_APIKEY));
        record.setId(IDUtil.genHttpProxyId(Plugin.serverId(), userId));
        record.setUserId(userId);

        record.setReqMethod(msg.method().name());
        record.setReqProtocol(msg.protocolVersion().protocolName());
        Integer contentLen = msg.headers().getInt(HttpHeaderNames.CONTENT_LENGTH);
        record.setReqSize(contentLen == null ? 0 : contentLen);//
        record.setReqTime(System.currentTimeMillis());

        URI uri = URI.create(msg.uri());
        record.setReqScheme(uri.getScheme());
        record.setReqHost(uri.getHost());
        record.setReqPort(uri.getPort() < 0 ? 80 : uri.getPort());
        record.setReqPath(uri.getPath());
        record.setReqQuery(uri.getQuery());

        record.setReqPolicy(((HttpRequest) msg).headers().get(ApiConst.HTTPPROXY_POLICY));

        record.setNodeId(nodeId);
        record.setProxyStatus(proxyStatus);

        return record;
    }

}
