package tbcloud.node.job.impl;

import com.google.gson.reflect.TypeToken;
import jframe.core.msg.Msg;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.DeviceVendorEnum;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.MacUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.job.NodeJob;
import tbcloud.node.model.MacSpace;
import tbcloud.node.model.NodeDevice;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.protocol.data.WifiDeviceInfo;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO 批量入库
 *
 * @author dzh
 * @date 2019-03-28 15:40
 */
public class NodeDeviceJob extends NodeJob {
    @Override
    public int msgType() {
        return MsgType.NODE_WIFI_DEVICE;
    }


    static Type DevicesType = new TypeToken<List<WifiDeviceInfo.DeviceInfo>>() {
    }.getType();

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        WifiDeviceInfo wifiDeviceInfo = null;
        if (val instanceof String) {  // from mq
            wifiDeviceInfo = GsonUtil.fromJson((String) val, WifiDeviceInfo.class);
        } else if (val instanceof NodeInfo) {
            wifiDeviceInfo = (WifiDeviceInfo) val;
        }

        if (wifiDeviceInfo == null)
            return;

        String nodeId = wifiDeviceInfo.getNodeId();
        List<WifiDeviceInfo.DeviceInfo> devices = GsonUtil.fromJson(wifiDeviceInfo.getDevice(), DevicesType);

        if (devices != null) {
            List<NodeDevice> inserted = new LinkedList<>();
            List<NodeDevice> updated = new LinkedList<>();

            for (WifiDeviceInfo.DeviceInfo dev : devices) {
                if (StringUtil.isEmpty(dev.getMac())) continue;

                NodeDevice r = record(dev);
                if (r != null) r.setNodeId(nodeId);

                if (NodeDao.selectNodeDevice(r.getMac()) == null) {
                    inserted.add(r);
                } else {
                    updated.add(r);
                }
            }

            if (!inserted.isEmpty()) {
                NodeDao.batchInsertNodeDevice(inserted);
            }
            if (!updated.isEmpty()) {
                NodeDao.batchUpdateNodeDevice(updated);
            }
        }
    }

    private NodeDevice record(WifiDeviceInfo.DeviceInfo dev) {
        String mac = dev.getMac();

        NodeDevice r = new NodeDevice();
        r.setMac(mac);
        r.setName(dev.getName());
        r.setOnTime(dev.getOnTime());
        r.setOffTime(dev.getOffTime());

        if (dev.getOnTime() != null && dev.getOnTime() > 0) {
            r.setStatus(ApiConst.IS_ONLINE);
        } else {
            r.setStatus(ApiConst.IS_OFFLINE);
        }

        if (!StringUtil.isEmpty(mac)) {
            DeviceVendorEnum deviceVendorEnum = DeviceVendorEnum.Default;

            mac = MacUtil.clean(mac).substring(0, 6);
            MacSpace macSpace = NodeDao.selectMacSpace(MacUtil.macValue(mac));
            if (macSpace != null) {
                String com = macSpace.getCompany();
                if (!StringUtil.isEmpty(com)) {
                    com = com.toLowerCase();
                    DeviceVendorEnum[] devs = DeviceVendorEnum.values();
                    for (int i = 0; i < devs.length; ++i) {
                        if (com.indexOf(devs[i].getCompany().toLowerCase()) >= 0) {
                            deviceVendorEnum = devs[i];
                            break;
                        }
                    }
                }
            }

            r.setMacVendor(deviceVendorEnum.getCompany());
            r.setMacIcon(deviceVendorEnum.getIcon());
        }

        return r;
    }

}
