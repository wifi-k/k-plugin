package tbcloud.node.httpproxy.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.ByteBufHttpProxy;
import tbcloud.node.protocol.PacketConst;

/**
 * Magic[4] + Version[4] + IdSize[1] + Id[] + TokenSize[1] + Token[] +  DataType[4] + DataFormat[1] + DataSize[4] + Data[] + Hash[8]
 *
 * @author dzh
 * @date 2018-12-14 00:01
 */
@ChannelHandler.Sharable
public class HttpProxyEncoder extends MessageToByteEncoder<ByteBufHttpProxy> {

    static Logger LOG = LoggerFactory.getLogger(HttpProxyEncoder.class);

//    static final PacketCodec codec = new PacketCodecV20181130(); //TODO

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBufHttpProxy msg, ByteBuf out) throws Exception {
        //LOG.info("HttpProxyEncoder encode {}", msg.id());
        out.writeInt(msg.magic());
        out.writeInt(msg.version());

        String id = msg.id();
        out.writeByte(id.length());
        out.writeBytes(id.getBytes(PacketConst.UTF_8));

        String token = msg.token();
        out.writeByte(token.length());
        out.writeBytes(token.getBytes(PacketConst.UTF_8));

        out.writeInt(msg.dataType());
        out.writeByte(msg.dataFormat());
        byte[] data = msg.data().array();
        out.writeInt(data.length);
        out.writeBytes(data);
        out.writeLong(0L); //TCP的时候忽略hash值
//        out.writeBytes(codec.encode(msg).array());

        LOG.info("write {} {}", msg.id(), msg.dataType());
    }

}
