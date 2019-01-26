package tbcloud.node.httpproxy.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.httpproxy.protocol.ByteBufHttpProxy;
import tbcloud.node.protocol.PacketConst;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * transfer ByteBuf into {@link ByteBufHttpProxy}
 * <p>
 * Magic[4] + Version[4] + IdSize[1] + Id[] + TokenSize[1] + Token[] +  DataType[4] + DataFormat[1] + DataSize[4] + Data[] + Hash[8]
 *
 * @author dzh
 * @date 2018-12-13 23:58
 */
public class HttpProxyDecoder extends ByteToMessageDecoder {

    static Logger LOG = LoggerFactory.getLogger(HttpProxyDecoder.class);

    static final int PT_M = 0, PT_ID = 1, PT_TOKEN = 2, PT_DATA = 3;

    int readType = PT_M;// 0-magic 1-id 2-token 3-data
    int readSize = 4;

    private ByteBufHttpProxy msg = null;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        resetDecode();
        super.handlerAdded(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (readType) { // pending
            case PT_M:
                if (msg == null && in.readableBytes() >= readSize + 5) { // m + v + idSize
                    int m = in.readInt(); //Magic
                    if (m != PacketConst.M) {
                        in.discardReadBytes(); //TODO
                        ctx.close(); //close channel
                        return;
                    }

                    msg = new ByteBufHttpProxy();
                    msg.magic(m);
                    msg.version(in.readInt());//Version TODO check version
                    readType = PT_ID;
                    readSize = in.readByte();//IdSize
                } else break;
            case PT_ID:
                if (in.readableBytes() >= readSize + 1) {
                    msg.id(in.readCharSequence(readSize, PacketConst.UTF_8).toString()); //Id
                    readType = PT_TOKEN;
                    readSize = in.readByte(); // TokenSize
                } else break;
            case PT_TOKEN:
                if (in.readableBytes() >= readSize + 9) {
                    msg.token(in.readCharSequence(readSize, PacketConst.UTF_8).toString());//Token
                    msg.dataType(in.readInt());//DataType
                    msg.dataFormat(in.readByte());//DataFormat
                    readType = PT_DATA;
                    readSize = in.readInt();//DataSize
                } else break;
            case PT_DATA:
                if (in.readableBytes() >= readSize + 8) {
                    byte[] data = new byte[readSize];
                    in.readBytes(data);
                    msg.data(ByteBuffer.wrap(data)); //Data
                    msg.hash(in.readLong());//Hash

                    out.add(msg);
                    LOG.info("read msg m-{} v-{} id-{} t-{} dt-{} df-{} s-{}", msg.magic(), msg.version(),
                            msg.id(), msg.token(), msg.dataType(), msg.dataFormat(), msg.data().capacity());
                    resetDecode();
                } else break;
        }
    }

    /**
     * 重置数据包解析状态，开始从Magic解析
     */
    private void resetDecode() {
        msg = null;
        readType = PT_M;
        readSize = 4;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        resetDecode();
        super.exceptionCaught(ctx, cause);
    }

}
