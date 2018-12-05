package tbcloud.node.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.node.protocol.ByteBufNodePacket;
import tbcloud.node.protocol.DataType;
import tbcloud.node.protocol.codec.DataCodec;
import tbcloud.node.protocol.data.DataRsp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * @author dzh
 * @date 2018-11-28 15:55
 */
public class IoContext {

    static Logger LOG = LoggerFactory.getLogger(IoContext.class);

    private IoDispatch dispatch;
    private DatagramChannel ch;
    private ByteBufNodePacket req;
    private SocketAddress remote;

    public IoContext(IoDispatch dispatch, DatagramChannel chanenl, ByteBufNodePacket req, SocketAddress remote) {
        this.dispatch = dispatch;
        this.ch = chanenl;
        this.req = req;
        this.remote = remote;
    }

    public DatagramChannel channel() {
        return ch;
    }

    public ByteBufNodePacket request() {
        return req;
    }

    public SocketAddress remote() {
        return remote;
    }

    private ByteBufNodePacket toRsp(DataRsp data) {
        ByteBufNodePacket rsp = new ByteBufNodePacket();
        rsp.magic(req.magic()).version(req.version()).id(req.id()).dataType(DataType.I.rspType(req.dataType()))
                .dataFormat(req.dataFormat());
        rsp.data(dataCodec().encode(data));//TODO data format rsp
        return rsp;
    }

    public void write(DataRsp<?> data) throws IOException {
        ByteBufNodePacket rsp = toRsp(data);
        LOG.info("write {} {}", rsp.id(), data);
        ch.send(dispatch.server().codec().encode(rsp), remote);
    }

    public void write(int code) throws IOException {
        DataRsp<Void> data = new DataRsp<>();
        data.setCode(code);
        write(data);
    }

    public DataCodec dataCodec() {
        return dispatch.dataCodecFactory().codec(req.dataType(), req.dataFormat());
    }

}
