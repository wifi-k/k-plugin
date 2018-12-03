package tbcloud.node.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiCode;
import tbcloud.node.protocol.ByteBufNodePacket;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.codec.PacketCodec;
import tbcloud.node.protocol.codec.PacketCodecV20181130;
import tbcloud.node.protocol.data.DataRsp;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.zip.CRC32;

/**
 * @author dzh
 * @date 2018-11-27 14:32
 */
@Deprecated
public class NodeApiServer implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(NodeApiServer.class);

    private SocketAddress listenAddr; //服务端地址
    private boolean authEnabled; //认证节点功能是否启用

    private volatile boolean closed = false;

    private PacketCodec codec = new PacketCodecV20181130();   //TODO conf

    private IoDispatch dispatch;

    private DatagramChannel ch;

    public PacketCodec codec() {
        return this.codec;
    }

    NodeApiServer(SocketAddress addr, boolean authEnabled) {
        this.listenAddr = addr;
        this.authEnabled = authEnabled;

        //this.dispatch = new IoDispatch(this);
    }

    void start() throws IOException {
        ch = newChannel();
        startSelectThreaad(ch);
    }

    private DatagramChannel newChannel() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.setOption(StandardSocketOptions.SO_RCVBUF, PacketConst.MAX_SIZE);
        channel.setOption(StandardSocketOptions.SO_SNDBUF, PacketConst.MAX_SIZE);
        channel.setOption(StandardSocketOptions.SO_REUSEPORT, true); // TODO LBS
        channel.socket().bind(listenAddr);

        return channel;
    }

    private void startSelectThreaad(final DatagramChannel ch) {
        Thread selectT = new Thread(() -> {
            CRC32 crc32 = new CRC32();
            ByteBuffer recvBuf = ByteBuffer.allocate(PacketConst.MAX_SIZE);
            while (true) {
                if (isClosed() || !ch.isOpen()) break;

                recvBuf.clear();
                try {
                    SocketAddress remote = ch.receive(recvBuf);
                    //LOG.info("recvBuf {} {} {}", recvBuf.position(), recvBuf.limit(), recvBuf.capacity());
                    recvBuf.flip();    //TODO
                    LOG.info("recvBuf {} {} {}", recvBuf.position(), recvBuf.limit(), recvBuf.capacity());
                    // calc crc32
                    crc32.update(recvBuf.array(), 0, recvBuf.limit() - 8);
                    long hash = crc32.getValue();
                    ByteBufNodePacket req = codec.decode(recvBuf);
                    LOG.info("{} -> {} {} {} {}", remote, req.version(), req.id(), req.token(), req.dataType());

                    IoContext context = dispatch.context(ch, req, remote);
                    if (hash != req.hash()) { // crc is diff
                        DataRsp rsp = new DataRsp();
                        rsp.setCode(ApiCode.HASH_CODE_DIFF);

                        context.write(rsp);
                        LOG.warn("{} hash not equals {} {}", req.id(), req.hash(), hash);
                        continue;
                    }
                    // dispatch
                    dispatch.dispatch(context);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    crc32.reset();
                }

            }

            if (isClosed())
                LOG.info("{} exited", Thread.currentThread().getName());
            else
                LOG.error("{} exited abnormally", Thread.currentThread().getName());
        }, "NodeApiServer-select");
        selectT.start();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        try {
            closeChannel();
        } finally {
            dispatch.close();
        }
    }

    private void closeChannel() throws IOException {
        ch.close();
    }
}
