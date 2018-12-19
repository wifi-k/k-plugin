package tbcloud.node.httpproxy.dispatch;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.httpproxy.protocol.data.HttpProxyRequest;
import tbcloud.httpproxy.protocol.data.HttpProxyResponse;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.Result;
import tbcloud.node.httpproxy.http.HttpToTcpHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * @author dzh
 * @date 2018-12-16 17:58
 */
public class DispatchNode implements AutoCloseable {

    static Logger LOG = LoggerFactory.getLogger(DispatchNode.class);

    private ChannelHandlerContext tcpContext;

    private List<DispatchRecord> httpContext;

    public DispatchNode(ChannelHandlerContext tcpContext) {
        this.tcpContext = tcpContext;
        this.httpContext = new LinkedList<>();
    }

    public ChannelHandlerContext getTcpContext() {
        return tcpContext;
    }

    public void setTcpContext(ChannelHandlerContext tcpContext) {
        this.tcpContext = tcpContext;
    }

    public List<DispatchRecord> getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(List<DispatchRecord> httpContext) {
        this.httpContext = httpContext;
    }

    @Override
    public void close() throws Exception {
        synchronized (httpContext) {
            httpContext.forEach(r -> {
                try {
                    r.close();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            });
        }
    }

    public DispatchRecord findDispatchRecord(String recordId) {
        synchronized (httpContext) {
            for (DispatchRecord r : httpContext) {
                if (r.getId().equals(recordId))
                    return r;
            }
        }
        return null;
    }

    public void addDispatchRecord(DispatchRecord record) {
        synchronized (httpContext) { //TODO 重复
            httpContext.add(record);
        }
    }

    public void removeDispatchRecord(String recordId) {
        synchronized (httpContext) {
            httpContext.removeIf(r -> {
                boolean bool = r.getId().equals(recordId);
                if (bool) {
                    try {
                        r.close(); //
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                return bool;
            });
        }
    }

    public void writeRequest(ChannelHandlerContext httpContext, final HttpProxyRequest request) {
        if (request.getSeq() == HttpProxyConst.SEQ_LAST_NUM) {
            tcpContext.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        DispatchRecord record = new DispatchRecord();
                        record.setId(request.getId());
                        record.setHttpContext(httpContext);

                        record.setRecord(new HttpProxyRecord());
                        record.getRecord().setId(request.getId());
                        record.getRecord().setProxySendTime(System.currentTimeMillis());

                        addDispatchRecord(record);
                    } else {
                        Result<Void> r = new Result<>();
                        r.setCode(ApiCode.IO_ERROR);
                        r.setMsg("failed to write request");
                        HttpToTcpHandler.writeError(httpContext, r);
                    }
                }
            });
        } else {
            tcpContext.write(request);
        }
    }

    public void writeResponse(HttpProxyResponse response) {
        DispatchRecord record = findDispatchRecord(response.getId());
        if (record == null)
            return;

        if (response.getSeq() == HttpProxyConst.SEQ_LAST_NUM) {
            record.getRecord().setProxyRecvTime(System.currentTimeMillis());
            record.getRecord().setProxyCost(response.getProxyCost());

            record.getHttpContext().writeAndFlush(Unpooled.wrappedBuffer(response.getHttp()))
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            // clear request record
                            removeDispatchRecord(record.getId());
                        }
                    });
        } else {
            record.getHttpContext().write(response);
        }
    }
}
