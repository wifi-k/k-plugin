package tbcloud.node.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.node.api.handler.AuthHandler;
import tbcloud.node.api.handler.HeartbeatHandler;
import tbcloud.node.api.handler.InsStatusHandler;
import tbcloud.node.api.handler.MonitorInfoHandler;
import tbcloud.node.protocol.ByteBufNodePacket;
import tbcloud.node.protocol.DataType;
import tbcloud.node.protocol.codec.DataCodecFactory;
import tbcloud.node.protocol.codec.DefaultDataCodecFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author dzh
 * @date 2018-11-29 12:14
 */
public class IoDispatch implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(IoDispatch.class);

    private NodeApiServerNio server;

    private DataCodecFactory dataCodecFactory = new DefaultDataCodecFactory(); //TODO

    // TODO
    private ExecutorService dispatchThread = Executors.newSingleThreadExecutor();

    // TODO
    private ExecutorService handlerThread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public IoDispatch(NodeApiServerNio server) {
        this.server = server;
    }

    public DataCodecFactory dataCodecFactory() {
        return this.dataCodecFactory;
    }

    public NodeApiServerNio server() {
        return this.server;
    }

    public void dispatch(final IoContext context) {
        dispatchThread.submit(() -> {
            ByteBufNodePacket req = context.request();
            int dataType = req.dataType();
            switch (dataType) {
                case DataType.AUTH:
                    handlerThread.submit(new AuthHandler(context));
                    break;
                case DataType.HEARTBEAT:
                    handlerThread.submit(new HeartbeatHandler(context));
                    break;
                case DataType.MONITOR:
                    handlerThread.submit(new MonitorInfoHandler(context));
                    break;
                case DataType.INS_STATUS:
                    handlerThread.submit(new InsStatusHandler(context));
                    break;
                default:
                    LOG.warn("unknown packet {} {} {}", req.id(), req.token(), req.dataType());
            }
        });
    }

    public IoContext context(DatagramChannel chanenl, ByteBufNodePacket req, SocketAddress remote) {
        return new IoContext(this, chanenl, req, remote);
    }

    @Override
    public void close() throws IOException {
        dispatchThread.shutdown();
        try { //TODO
            dispatchThread.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }

        handlerThread.shutdown();
        try {
            handlerThread.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }

    }

}
