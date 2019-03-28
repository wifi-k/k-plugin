package tbcloud.node.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.node.api.handler.*;
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dzh
 * @date 2018-11-29 12:14
 */
public class IoDispatch implements Closeable {

    static Logger LOG = LoggerFactory.getLogger(IoDispatch.class);

    private NodeApiServerNio server;

    private DataCodecFactory dataCodecFactory = new DefaultDataCodecFactory(); //TODO

    // TODO reject
    private ExecutorService handlerThread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1, new DefaultThreadFactory());

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
        try {
            ByteBufNodePacket req = context.request();
            int dataType = req.dataType();
            switch (dataType) {
                case DataType.AUTH:
                    handlerThread.submit(new AuthHandler(context)).get(1, TimeUnit.SECONDS); //TODO
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
                case DataType.DEVICE:
                    handlerThread.submit(new DeviceInfoHandler(context));
                    break;
                default:
                    LOG.warn("unknown packet {} {} {}", req.id(), req.token(), req.dataType());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e); //TODO
        }
        //LOG.info("dispatch {}", context.request().id());
    }

    public IoContext context(DatagramChannel chanenl, ByteBufNodePacket req, SocketAddress remote) {
        return new IoContext(this, chanenl, req, remote);
    }

    @Override
    public void close() throws IOException {
        handlerThread.shutdown();
        try {
            handlerThread.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "IoDispatch-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}


