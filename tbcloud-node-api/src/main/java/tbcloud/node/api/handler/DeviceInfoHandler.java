package tbcloud.node.api.handler;

import jframe.core.msg.TextMsg;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.node.api.IoContext;
import tbcloud.node.protocol.data.DataRsp;
import tbcloud.node.protocol.data.WifiDeviceInfo;

/**
 * @author dzh
 * @date 2019-03-28 15:00
 */
public class DeviceInfoHandler extends DataHandler<WifiDeviceInfo> {

    public DeviceInfoHandler(IoContext context) {
        super(context);
    }

    @Override
    protected DataRsp<?> handle(WifiDeviceInfo dataReq) {
        String nodeId = dataReq.getNodeId();

        String device = dataReq.getDevice();
        if (null != device) {
            Plugin.sendToNode(new TextMsg().setType(MsgType.NODE_WIFI_DEVICE).setValue(GsonUtil.toJson(dataReq)), nodeId);
        }

        return SUCC;
    }

    @Override
    protected WifiDeviceInfo decodeDataReq(IoContext context) {
        //LOG.info("data {}", new String(context.request().data().array(), PacketConst.UTF_8));
        return context.dataCodec().decode(context.request().data(), WifiDeviceInfo.class);
    }
}
