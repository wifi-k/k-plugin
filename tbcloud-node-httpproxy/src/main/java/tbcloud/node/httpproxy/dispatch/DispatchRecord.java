package tbcloud.node.httpproxy.dispatch;

import io.netty.channel.ChannelHandlerContext;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import tbcloud.httpproxy.dao.service.HttpProxyDaoService;
import tbcloud.httpproxy.model.HttpProxyRecord;

/**
 * @author dzh
 * @date 2018-12-16 17:56
 */
@Injector
public class DispatchRecord implements AutoCloseable {

    private String id; //record id
    private ChannelHandlerContext httpContext;
    private HttpProxyRecord record;

    @InjectService(id = "tbcloud.service.httpproxy.dao")
    static HttpProxyDaoService HttpProxyDao;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ChannelHandlerContext getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(ChannelHandlerContext httpContext) {
        this.httpContext = httpContext;
    }

    public HttpProxyRecord getRecord() {
        return record;
    }

    public void setRecord(HttpProxyRecord record) {
        this.record = record;
    }

    @Override
    public void close() throws Exception {
        if (httpContext != null)
            httpContext.channel().close();

        if (record != null)
            HttpProxyDao.updateHttpProxyRecord(record);
    }
}
