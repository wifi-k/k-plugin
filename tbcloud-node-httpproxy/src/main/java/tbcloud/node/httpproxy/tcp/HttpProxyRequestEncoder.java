package tbcloud.node.httpproxy.tcp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.ByteBufHttpProxy;
import tbcloud.httpproxy.protocol.HttpProxyConst;
import tbcloud.httpproxy.protocol.HttpProxyDataType;
import tbcloud.httpproxy.protocol.codec.HttpProxyDataCodecFactory;
import tbcloud.httpproxy.protocol.data.HttpProxyRequest;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.codec.DataCodecFactory;

import java.util.List;

/**
 * wrap {@link HttpProxyRequest} in {@ByteBufHttpProxy}
 *
 * @author dzh
 * @date 2018-12-16 14:54
 */
@ChannelHandler.Sharable
public class HttpProxyRequestEncoder extends MessageToMessageEncoder<HttpProxyRequest> {

    static final Logger LOG = LoggerFactory.getLogger(HttpProxyRequestEncoder.class);

    static final DataCodecFactory DataCodecFactory = new HttpProxyDataCodecFactory();

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpProxyRequest data, List<Object> out) throws Exception {
        LOG.info("HttpProxyRequestEncoder {} {}", data.getId(), data.getSeq());
        ByteBufHttpProxy msg = new ByteBufHttpProxy(); //push http request
        msg.magic(PacketConst.M);
        msg.version(PacketConst.V_20181130);
        msg.id(data.getId()); // TODO
        msg.token("");
        msg.dataType(HttpProxyDataType.PUSH_HPROXY_REQUEST);
        msg.dataFormat((byte) PacketConst.DataFormat.BINARY.ordinal());
        msg.data(DataCodecFactory.codec(msg.dataType(), msg.dataFormat()).encode(data));
        msg.hash(0L);

        out.add(msg);

        LOG.info("push {} {} {} {}://{}:{} ", data.getNodeId(), data.getId(), data.getSeq(),
                data.getScheme() == HttpProxyConst.SCHEME_HTTPS ? "https" : "http", data.getHost(), data.getPort());
    }

}
