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
import tbcloud.node.model.*;
import tbcloud.node.protocol.data.WifiDeviceInfo;
import tbcloud.user.model.UserMessage;
import tbcloud.user.model.UserNode;
import tbcloud.user.model.UserNodeExample;
import tbcloud.user.model.UserOnline;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 设备上下线
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

        // 直接处理上下线
        devices.forEach(devInfo -> {
            NodeDevice device = record(devInfo);
            device.setNodeId(nodeId);

            String devName = device.getName();
            boolean push = false;
            boolean newDev = false;

            NodeDeviceExample example = new NodeDeviceExample();
            example.createCriteria().andNodeIdEqualTo(nodeId).andMacEqualTo(device.getMac()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
            List<NodeDevice> list = NodeDao.selectNodeDevice(example);
            if (list == null || list.isEmpty()) {
                NodeDao.insertNodeDevice(device);
                newDev = true;
            } else {
                NodeDao.updateNodeDeviceSelective(device, example);
                if (StringUtil.isEmpty(devName))
                    devName = list.get(0).getNote();


                if (list.get(0).getIsOnline() == ApiConst.IS_SELECT_Y) push = true;
            }

            UserNodeExample userNodeExample = new UserNodeExample();
            userNodeExample.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
            List<UserNode> users = UserDao.selectUserNode(userNodeExample);
            if (users.isEmpty()) return;

            //TODO msg async
            if (device.getStatus() == ApiConst.IS_ONLINE) {  // online
                if (push) { //发送给所有的家庭成员
                    pushOnlineNotification(users, device.getNodeId(), devName);
                }
                insertUserMessageOnline(users, devName, newDev ? "【新设备上线】" : "【设备上线】");
            } else {
                // offline
                insertUserMessageOffline(users, devName, newDev ? "【设备下线】" : "【设备上线】");
            }

            insertNodeDeviceRecord(device);
        });

        // 由wifi判断好上下线,下面的代码没用了
//        Map<String, WifiDeviceInfo.DeviceInfo> devicesMap = toMap(devices); //current online devices
//        if (devices != null) {
//            NodeDeviceExample example = new NodeDeviceExample();
//            example.createCriteria().andNodeIdEqualTo(nodeId).andStatusEqualTo(ApiConst.IS_ONLINE).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
//            example.setOrderByClause("on_time limit 1000");
//            List<NodeDevice> prevOnlineNodes = NodeDao.selectNodeDevice(example);
//
//            List<NodeDevice> onlineNodes = new LinkedList<>();
//            List<NodeDevice> offlineNodes = new LinkedList<>();
//            for (NodeDevice dev : prevOnlineNodes) {
//                WifiDeviceInfo.DeviceInfo currentOnline = devicesMap.remove(dev.getMac());
//                if (currentOnline != null) { //online
//                    NodeDevice r = record(currentOnline);
//                    r.setId(dev.getId());
//                    onlineNodes.add(r);
//                } else { //offline
//                    dev.setStatus(ApiConst.IS_OFFLINE);
//                    dev.setOffTime(System.currentTimeMillis());
//                    offlineNodes.add(dev);
//                    // TODO send offline msg
//                }
//            }
//            NodeDao.batchUpdateNodeDevice(onlineNodes);
//            NodeDao.batchUpdateNodeDevice(offlineNodes);
//
//            for (WifiDeviceInfo.DeviceInfo dev : devicesMap.values()) {
//                if (StringUtil.isEmpty(dev.getMac())) continue;
//
//                NodeDevice r = record(dev);
//                r.setNodeId(nodeId);
//
//                example = new NodeDeviceExample();
//                example.createCriteria().andNodeIdEqualTo(nodeId).andMacEqualTo(dev.getMac()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
//                List<NodeDevice> list = NodeDao.selectNodeDevice(example);
//                if (list == null || list.isEmpty()) {
//                    NodeDao.insertNodeDevice(r);
//                } else {
//                    r.setId(list.get(0).getId());
//                    NodeDao.updateNodeDevice(r);
//                }
//            }
//        }
    }

    private void insertNodeDeviceRecord(NodeDevice device) {
        NodeDeviceRecord record = new NodeDeviceRecord();
        record.setNodeId(device.getNodeId());
        record.setMac(device.getMac());
        int status = device.getStatus();
        record.setStatus(status);
        record.setTime(status == ApiConst.IS_ONLINE ? device.getOnTime() : device.getOffTime());

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(record.getTime());
        record.setYear(c.get(Calendar.YEAR));
        record.setWeek(c.get(Calendar.WEEK_OF_YEAR));
        record.setDay(c.get(Calendar.DAY_OF_YEAR));

        NodeDao.insertNodeDeviceRecord(record);
    }

    private void insertUserMessageOnline(List<UserNode> users, String devName, String title) {
        List<UserMessage> list = new ArrayList<>(users.size());
        for (UserNode userNode : users) {
            UserMessage message = new UserMessage();
            message.setUserId(userNode.getUserId());
            message.setTitle(title);
            message.setContent(devName + "上线啦");

            list.add(message);
        }
        UserDao.batchInsertUserMessage(list);
    }

    private void insertUserMessageOffline(List<UserNode> users, String devName, String title) {
        List<UserMessage> list = new ArrayList<>(users.size());
        for (UserNode userNode : users) {
            UserMessage message = new UserMessage();
            message.setUserId(userNode.getUserId());
            message.setTitle(title);
            message.setContent(devName + "下线"); //TODO 文案

            list.add(message);
        }
        UserDao.batchInsertUserMessage(list);
    }

    private void pushOnlineNotification(List<UserNode> users, String nodeId, String devName) {
        if (StringUtil.isEmpty(nodeId)) return;

        for (UserNode userNode : users) {
            // send
            UserOnline userOnline = UserDao.selectUserOnline(userNode.getUserId());
            if (userOnline.getDevType() == 0 || StringUtil.isEmpty(userOnline.getDevToken())) {
                continue;
            }

            int devType = userOnline.getDevType();
            try {
                if (ApiConst.DEV_TYPE_IOS == devType) {
                    Umeng.sendIOSUnicast(ApiConst.UMENG_ID_IOSU, userOnline.getDevToken(), "【设备上线】" + devName + "上线啦", null, null, null);
                } else if (ApiConst.DEV_TYPE_AND == devType) {

                    Umeng.sendAndUnicast(ApiConst.UMENG_ID_IOSU, userOnline.getDevToken(), "xiaok通知", "【设备上线】", devName + "上线啦", null);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private Map<String, WifiDeviceInfo.DeviceInfo> toMap(List<WifiDeviceInfo.DeviceInfo> devices) {
        if (devices == null || devices.isEmpty()) return Collections.emptyMap();

        Map<String, WifiDeviceInfo.DeviceInfo> deviceInfoMap = new HashMap<>();
        for (WifiDeviceInfo.DeviceInfo dev : devices) {
            deviceInfoMap.put(dev.getMac(), dev);
        }
        return deviceInfoMap;
    }

    private NodeDevice record(WifiDeviceInfo.DeviceInfo dev) {
        String mac = dev.getMac();

        NodeDevice r = new NodeDevice();
        r.setMac(mac);
        r.setName(dev.getName());
        r.setOnTime(dev.getOnTime());
        r.setOffTime(dev.getOffTime());
        r.setLocalIp(dev.getIp());

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
