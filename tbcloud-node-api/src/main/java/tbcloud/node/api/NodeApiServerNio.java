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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.zip.CRC32;

/**
 * TODO
 *
 * @author dzh
 * @date 2018-11-27 14:32
 */
public class NodeApiServerNio implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(NodeApiServerNio.class);

    private SocketAddress listenAddr; //服务端地址
    private boolean authEnabled; //认证节点功能是否启用

    private Selector sel;

    private volatile boolean closed = false;

    private PacketCodec codec = new PacketCodecV20181130();   //TODO conf

    private IoDispatch dispatch;

    public PacketCodec codec() {
        return this.codec;
    }

    NodeApiServerNio(SocketAddress addr, boolean authEnabled) {
        this.listenAddr = addr;
        this.authEnabled = authEnabled;

        // TODO
        this.dispatch = new IoDispatch(this);
    }

    void start() throws IOException {
        sel = Selector.open();

        registerChannel(sel);
        startSelectThreaad(sel);
    }

    private void registerChannel(Selector sel) throws IOException {
        int n = 1;//Runtime.getRuntime().availableProcessors();
        for (var i = 0; i < n; i++) {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_RCVBUF, PacketConst.MAX_SIZE);
            channel.setOption(StandardSocketOptions.SO_SNDBUF, PacketConst.MAX_SIZE);
            channel.setOption(StandardSocketOptions.SO_REUSEPORT, true); // TODO LBS
            channel.socket().bind(listenAddr);

            channel.register(sel, SelectionKey.OP_READ, ByteBuffer.allocate(PacketConst.MAX_SIZE));
        }
    }

    private void startSelectThreaad(final Selector sel) {
        Thread selectT = new Thread(() -> {
            int n = 0;
            CRC32 crc32 = new CRC32();
            while (true) {
                if (isClosed() || !sel.isOpen()) break;

                try {
                    n = sel.select();
                    LOG.info("select {}", n);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                if (n > 0) {
                    Iterator<SelectionKey> iter = sel.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();

                        DatagramChannel ch = (DatagramChannel) key.channel();
                        ByteBuffer recvBuf = (ByteBuffer) key.attachment();
                        recvBuf.clear();
                        try {
                            SocketAddress remote = ch.receive(recvBuf);
                            recvBuf.flip();
                            //LOG.info("recvBuf {} {} {}", recvBuf.position(), recvBuf.limit(), recvBuf.capacity());
                            // calc crc32
                            crc32.update(recvBuf.array(), 0, recvBuf.limit() - 8);
                            long hash = crc32.getValue();
                            ByteBufNodePacket req = codec.decode(recvBuf);
                            if (req == null) {
                                LOG.debug("ignore {} req is null", remote);
                                continue;
                            }
                            LOG.info("{} -> {} {} {} {}", remote, req.version(), req.id(), req.token(), req.dataType());

                            IoContext context = dispatch.context(ch, req, remote);
                            if (hash != req.hash()) { // crc is diff
                                LOG.warn("{} hash {} not equal  {}", req.id(), req.hashCode(), hash);

                                DataRsp rsp = new DataRsp();
                                rsp.setCode(ApiCode.HASH_CODE_DIFF);

                                context.write(rsp);
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
            closeChannel(sel);
        } finally {
            dispatch.close();
        }
        LOG.info("{} closed", getClass().getName());
    }

    private void closeChannel(Selector sel) throws IOException {
        try {
            sel.wakeup();
            Iterator<SelectionKey> iter = sel.keys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                key.cancel();
                key.channel().close();
            }
        } finally {
            sel.close();
        }
    }
}
