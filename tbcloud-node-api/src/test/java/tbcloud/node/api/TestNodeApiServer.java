package tbcloud.node.api;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.model.NodeConst;
import tbcloud.node.protocol.ByteBufNodePacket;
import tbcloud.node.protocol.DataType;
import tbcloud.node.protocol.PacketConst;
import tbcloud.node.protocol.codec.DataCodecFactory;
import tbcloud.node.protocol.codec.DefaultDataCodecFactory;
import tbcloud.node.protocol.codec.PacketCodec;
import tbcloud.node.protocol.codec.PacketCodecV20181130;
import tbcloud.node.protocol.data.*;
import tbcloud.node.protocol.data.rsp.HeartbeatRsp;
import tbcloud.node.protocol.data.rsp.NodeAuthRsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.UUID;

/**
 * @author dzh
 * @date 2018-11-30 15:08
 */
public class TestNodeApiServer {

    static Logger LOG = LoggerFactory.getLogger(TestNodeApiServer.class);

    DataCodecFactory dataCodecFactory = DefaultDataCodecFactory.Instance;

    PacketCodec codec = new PacketCodecV20181130();

    InetSocketAddress serverAddr = new InetSocketAddress("47.98.51.82", 9019);

    //InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", 9019);

    private DatagramChannel ch;

    @Before
    public void initChannel() throws IOException {
        ch = DatagramChannel.open();
    }

    @Test
    public void authTest() throws IOException {
        String nodeId = "#A00000000000001";
        String id = UUID.randomUUID().toString();

        ByteBufNodePacket packet = new ByteBufNodePacket();
        packet.magic(PacketConst.M);
        packet.version(PacketConst.V_20181130);
        packet.id(id);
        //packet.token(IDUtil.genNodeToken(nodeId));

        NodeAuth data = new NodeAuth();
        data.setNodeId(nodeId);
        data.setManufactory("tbcloud");

        packet.dataFormat((byte) PacketConst.DataFormat.JSON.ordinal());
        packet.dataType(DataType.AUTH);
        ByteBuffer dataBuf = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).encode(data);
        packet.data(dataBuf);

        ByteBuffer bytes = codec.encode(packet);
        LOG.info("bytes {} {} {}", bytes.position(), bytes.limit(), bytes.capacity());
        int n = ch.send(bytes, serverAddr);
        LOG.info("send {} bytes {} {} {}", n, bytes.position(), bytes.limit(), bytes.capacity());

//        CRC32 crc = new CRC32();
//        crc.update(bytes.array(), 0, bytes.capacity() - 8);
//        LOG.info("{} crc32 {}", bytes.capacity(), crc.getValue());
//        crc.reset();

        ByteBuffer buf = ByteBuffer.allocate(PacketConst.MAX_SIZE);
        ch.receive(buf);
        buf.flip();
        packet = codec.decode(buf);
        LOG.info("crc32 {} {} ", packet.hash(), packet.data().limit());
        DataRsp<NodeAuthRsp> dataRsp = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).<DataRsp<NodeAuthRsp>>decode(packet.data(), DataRsp.class);
        LOG.info("{} {} {} {}", packet.id(), packet.token(), packet.token(), GsonUtil.toJson(dataRsp));
    }

    @Test
    public void heartbeatTest() throws IOException {
        String nodeId = "#A00000000000001";
        String token = "6a9ff9ff7dd4ecb8cb498772";
        String id = UUID.randomUUID().toString();

        ByteBufNodePacket packet = new ByteBufNodePacket();
        packet.magic(PacketConst.M);
        packet.version(PacketConst.V_20181130);
        packet.id(id);
        packet.token(token);

        Heartbeat data = new Heartbeat();
        data.setNodeId(nodeId);

        packet.dataFormat((byte) PacketConst.DataFormat.JSON.ordinal());
        packet.dataType(DataType.HEARTBEAT);
        ByteBuffer dataBuf = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).encode(data);
        packet.data(dataBuf);

        ByteBuffer bytes = codec.encode(packet);
        LOG.info("bytes {} {} {}", bytes.position(), bytes.limit(), bytes.capacity());
        int n = ch.send(bytes, serverAddr);
        LOG.info("send {} bytes {} {} {}", n, bytes.position(), bytes.limit(), bytes.capacity());

//        CRC32 crc = new CRC32();
//        crc.update(bytes.array(), 0, bytes.capacity() - 8);
//        LOG.info("{} crc32 {}", bytes.capacity(), crc.getValue());
//        crc.reset();

        ByteBuffer buf = ByteBuffer.allocate(PacketConst.MAX_SIZE);
        ch.receive(buf);
        buf.flip();
        packet = codec.decode(buf);
        LOG.info("crc32 {} {} ", packet.hash(), packet.data().limit());
        DataRsp<HeartbeatRsp> dataRsp = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).<DataRsp<HeartbeatRsp>>decode(packet.data(), DataRsp.class);
        LOG.info("{} {} {} {}", packet.id(), packet.token(), packet.token(), GsonUtil.toJson(dataRsp));
    }

    @Test
    public void monitoInfoTest() throws IOException {
        String nodeId = "dzh1123";
        String token = "7feef1f36443fcb6f8c17abc";
        String id = UUID.randomUUID().toString();

        ByteBufNodePacket packet = new ByteBufNodePacket();
        packet.magic(PacketConst.M);
        packet.version(PacketConst.V_20181130);
        packet.id(id);
        packet.token(token);

        MonitorInfo data = new MonitorInfo();
        data.setType(PacketConst.MONITOR_TYPE_SYS);
        data.setNodeId(nodeId);
        data.setCpuLoad(1);
        data.setCpuUsage(50);
        data.setDiskUsage(10);
        data.setMemUsage(60);

        packet.dataFormat((byte) PacketConst.DataFormat.JSON.ordinal());
        packet.dataType(DataType.MONITOR);
        ByteBuffer dataBuf = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).encode(data);
        packet.data(dataBuf);

        ByteBuffer bytes = codec.encode(packet);
        LOG.info("bytes {} {} {}", bytes.position(), bytes.limit(), bytes.capacity());
        int n = ch.send(bytes, serverAddr);
        LOG.info("send {} bytes {} {} {}", n, bytes.position(), bytes.limit(), bytes.capacity());

//        CRC32 crc = new CRC32();
//        crc.update(bytes.array(), 0, bytes.capacity() - 8);
//        LOG.info("{} crc32 {}", bytes.capacity(), crc.getValue());
//        crc.reset();

        ByteBuffer buf = ByteBuffer.allocate(PacketConst.MAX_SIZE);
        ch.receive(buf);
        buf.flip();
        packet = codec.decode(buf);
        LOG.info("crc32 {} {} ", packet.hash(), packet.data().limit());
        DataRsp<Void> dataRsp = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).<DataRsp<Void>>decode(packet.data(), DataRsp.class);
        LOG.info("{} {} {} {}", packet.id(), packet.token(), packet.token(), GsonUtil.toJson(dataRsp));
    }

    @Test
    public void insStatusTest() throws IOException {
        String nodeId = "dzh1123";
        String token = "7feef1f36443fcb6f8c17abc";
        String id = UUID.randomUUID().toString();

        ByteBufNodePacket packet = new ByteBufNodePacket();
        packet.magic(PacketConst.M);
        packet.version(PacketConst.V_20181130);
        packet.id(id);
        packet.token(token);

        InsStatus data = new InsStatus();
        data.setNodeId(nodeId);
        data.setId("1");
        data.setStatus(NodeConst.INS_STATUS_SUCC);

        packet.dataFormat((byte) PacketConst.DataFormat.JSON.ordinal());
        packet.dataType(DataType.INS_STATUS);
        ByteBuffer dataBuf = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).encode(data);
        packet.data(dataBuf);

        ByteBuffer bytes = codec.encode(packet);
        LOG.info("bytes {} {} {}", bytes.position(), bytes.limit(), bytes.capacity());
        int n = ch.send(bytes, serverAddr);
        LOG.info("send {} bytes {} {} {}", n, bytes.position(), bytes.limit(), bytes.capacity());

//        CRC32 crc = new CRC32();
//        crc.update(bytes.array(), 0, bytes.capacity() - 8);
//        LOG.info("{} crc32 {}", bytes.capacity(), crc.getValue());
//        crc.reset();

        ByteBuffer buf = ByteBuffer.allocate(PacketConst.MAX_SIZE);
        ch.receive(buf);
        buf.flip();
        packet = codec.decode(buf);
        LOG.info("crc32 {} {} ", packet.hash(), packet.data().limit());
        DataRsp<Void> dataRsp = dataCodecFactory.codec(packet.dataType(), packet.dataFormat()).<DataRsp<Void>>decode(packet.data(), DataRsp.class);
        LOG.info("{} {} {} {}", packet.id(), packet.token(), packet.token(), GsonUtil.toJson(dataRsp));
    }


}
