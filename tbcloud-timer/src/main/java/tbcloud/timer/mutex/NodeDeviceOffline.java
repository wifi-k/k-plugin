package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.DeviceVendorEnum;
import tbcloud.lib.api.util.MacUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.MacSpace;
import tbcloud.node.model.NodeDevice;
import tbcloud.node.model.NodeDeviceExample;

import java.util.ArrayList;
import java.util.List;

/**
 * offline: online and  onTime < now() - 10Min
 *
 * @author dzh
 * @date 2019-03-28 17:09
 */
public class NodeDeviceOffline extends MutexTimer {

    @Override
    protected int doTimer() {

        NodeDeviceExample example = new NodeDeviceExample();
        example.createCriteria().andStatusEqualTo(ApiConst.IS_ONLINE).andOnTimeLessThan(System.currentTimeMillis() - 600000);
        example.setOrderByClause("on_time limit 1000");

        List<NodeDevice> devices = NodeDao.selectNodeDevice(example);

        List<NodeDevice> updated = new ArrayList<>(devices.size());
        devices.forEach(dev -> {
            NodeDevice up = new NodeDevice();
            up.setMac(dev.getMac());
            up.setStatus(ApiConst.IS_OFFLINE);

            String mac = up.getMac();
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

                up.setMacVendor(deviceVendorEnum.getCompany());
                up.setMacIcon(deviceVendorEnum.getIcon());
            }

            updated.add(up);
        });

        NodeDao.batchUpdateNodeDevice(updated);

        return devices.size();
    }

    @Override
    protected String path() {
        return "/node/device/offline";
    }

    @Override
    protected long delayMs() {
        return 300 * 1000;
    }
}
