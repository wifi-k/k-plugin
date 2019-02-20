package tbcloud.node.httpproxy.dispatch;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.httpproxy.protocol.data.HttpProxyRequest;
import tbcloud.httpproxy.protocol.data.HttpProxyResponse;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.Result;

import java.nio.ByteBuffer;
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
        if (tcpContext != null) tcpContext.close();
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
            httpContext.removeIf(r -> {  //TODO opt search node同时代理的http不会很多暂时这样
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
        LOG.info("write req {} {}", request.getId(), request.getSeq());
        if (request.getSeq() == HttpProxyConst.SEQ_LAST_NUM) {
            tcpContext.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    DispatchRecord record = new DispatchRecord();
                    record.setId(request.getId());
                    record.setHttpContext(httpContext);

                    if (future.isSuccess()) {
                        addDispatchRecord(record);
                    } else {
                        Result<Void> r = new Result<>();
                        r.setCode(ApiCode.IO_ERROR);
                        r.setMsg("failed to write request");

                        record.writeError(r);
                    }
                }
            });
        } else {
            tcpContext.write(request);
        }
    }

    public void writeResponse(HttpProxyResponse response) { //TODO ProxyCost
        if (response == null) return;

        LOG.info("write rsp {} {} {}", response.getId(), response.getSeq(), response.getProxyStatus());
        DispatchRecord record = findDispatchRecord(response.getId());
        if (record == null) {
            LOG.error("not found record {}", response.getId());
            return;
        }

        Result<Void> r = new Result<>();
        ByteBuffer httpContent = response.getHttp();
        if (response.getProxyStatus() == HttpProxyConst.PROXY_STATUS_FAIL) {
            r.setCode(ApiCode.IO_ERROR);
            r.setMsg("proxy failed");
        } else if (response.getProxyStatus() == HttpProxyConst.PROXY_STATUS_TIMEOUT) {
            r.setCode(ApiCode.RESPONSE_TIMEOUT);
            r.setMsg("proxy timeout");
        } else {
            if (httpContent == null || httpContent.array().length == 0) {
                r.setCode(ApiCode.IO_ERROR);
                r.setMsg("node's response is nil");
            }
        }

        if (r.getCode() != ApiCode.SUCC) {
            record.writeError(r, false);
            removeDispatchRecord(record.getId()); //close
            return;
        }

        LOG.info("write http {}", httpContent.asCharBuffer().toString());
        if (response.getSeq() == HttpProxyConst.SEQ_LAST_NUM) {
            record.getHttpContext().writeAndFlush(Unpooled.wrappedBuffer(httpContent))
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            // clear request record
                            removeDispatchRecord(record.getId());
                        }
                    });
        } else {
            record.getHttpContext().write(Unpooled.wrappedBuffer(httpContent));
        }
    }
}
