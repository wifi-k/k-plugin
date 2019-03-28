package tbcloud.timer.mutex;

import tbcloud.lib.api.ApiConst;
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
