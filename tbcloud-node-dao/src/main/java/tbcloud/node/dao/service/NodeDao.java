package tbcloud.node.dao.service;

import tbcloud.node.model.*;
import tbcloud.node.model.ext.NodeInfoRt;
import tbcloud.node.model.ext.NodeInfoRtExample;

import java.util.List;
import java.util.Set;

/**
 * @author dzh
 * @date 2018-11-12 17:16
 */
public interface NodeDao {

    // node_device_day
    NodeDeviceRecord selectNodeDeviceRecord(Long id);

    int insertNodeDeviceRecord(NodeDeviceRecord record);

    List<NodeDeviceRecord> selectNodeDeviceRecord(NodeDeviceRecordExample example);

    long countNodeDeviceRecord(NodeDeviceRecordExample example);

    int updateNodeDeviceRecord(NodeDeviceRecord record);

    int updateNodeDeviceRecordSelective(NodeDeviceRecord record, NodeDeviceRecordExample example);

    // node_device_week
    NodeDeviceWeek selectNodeDeviceWeek(Long id);

    void batchInsertNodeDeviceWeek(List<NodeDeviceWeek> devices);

    void batchUpdateNodeDeviceWeek(List<NodeDeviceWeek> devices);

    int insertNodeDeviceWeek(NodeDeviceWeek device);

    List<NodeDeviceWeek> selectNodeDeviceWeek(NodeDeviceWeekExample example);

    long countNodeDeviceWeek(NodeDeviceWeekExample example);

    int updateNodeDeviceWeek(NodeDeviceWeek device);

    int updateNodeDeviceWeekSelective(NodeDeviceWeek device, NodeDeviceWeekExample example);

    // node_device_day
    NodeDeviceDay selectNodeDeviceDay(Long id);

    int insertNodeDeviceDay(NodeDeviceDay device);

    List<NodeDeviceDay> selectNodeDeviceDay(NodeDeviceDayExample example);

    long countNodeDeviceDay(NodeDeviceDayExample example);

    int updateNodeDeviceDay(NodeDeviceDay device);

    int updateNodeDeviceDaySelective(NodeDeviceDay device, NodeDeviceDayExample example);

    // mac_space
    MacSpace selectMacSpace(long id);

    // node_device_allow
    NodeDeviceAllow selectNodeDeviceAllow(long id);

    int insertNodeDeviceAllow(NodeDeviceAllow deviceAllow);

    List<NodeDeviceAllow> selectNodeDeviceAllow(NodeDeviceAllowExample example);

    long countNodeDeviceAllow(NodeDeviceAllowExample example);

    int updateNodeDeviceAllow(NodeDeviceAllow deviceAllow);

    int updateNodeDeviceAllowSelective(NodeDeviceAllow deviceAllow, NodeDeviceAllowExample example);

    // node_device
    NodeDevice selectNodeDevice(Long id);

    void batchInsertNodeDevice(List<NodeDevice> devices);

    void batchUpdateNodeDevice(List<NodeDevice> devices);

    int insertNodeDevice(NodeDevice device);

    List<NodeDevice> selectNodeDevice(NodeDeviceExample example);

    long countNodeDevice(NodeDeviceExample example);

    int updateNodeDevice(NodeDevice device);

    int updateNodeDeviceSelective(NodeDevice device, NodeDeviceExample example);

    // node_wifi_timer
    NodeWifiTimer selectNodeWifiTimer(String nodeId);

    int insertNodeWifiTimer(NodeWifiTimer timer);

    List<NodeWifiTimer> selectNodeWifiTimer(NodeWifiTimerExample example);

    int updateNodeWifiTimer(NodeWifiTimer timer);

    int updateNodeWifiTimerSelective(NodeWifiTimer timer, NodeWifiTimerExample example);

    // node_firmware
    NodeFirmware selectNodeFirmware(long id);

    int insertNodeFirmware(NodeFirmware firmware);

    List<NodeFirmware> selectNodeFirmware(NodeFirmwareExample example);

    int updateNodeFirmware(NodeFirmware firmware);

    int updateNodeFirmwareSelective(NodeFirmware firmware, NodeFirmwareExample example);

    // node_wifi
    NodeWifi selectNodeWifi(long id);

    int insertNodeWifi(NodeWifi nodeWifi);

    List<NodeWifi> selectNodeWifi(NodeWifiExample example);

    int updateNodeWifi(NodeWifi nodeWifi);

    int updateNodeWifiSelective(NodeWifi nodeWifi, NodeWifiExample example);

    // node_app
    NodeApp selectNodeApp(long id);

    int insertNodeApp(NodeApp nodeApp);

    List<NodeApp> selectNodeApp(NodeAppExample example);

    int updateNodeApp(NodeApp nodeApp);

    int updateNodeAppSelective(NodeApp nodeApp, NodeAppExample example);

    // node_info
    int insertNodeInfo(NodeInfo nodeInfo);

    boolean batchInsertNodeInfo(Set<NodeInfo> nodeInfoList);

    List<NodeInfo> selectNodeInfo(NodeInfoExample example);

    NodeInfo selectNodeInfo(String nodeId);

    int updateNodeInfo(NodeInfo nodeInfo);

    long countNodeInfo(NodeInfoExample example);

    void batchUpdateNodeInfo(List<NodeInfo> nodeList);

    // node_rt
    List<NodeInfoRt> selectNodeInfoLeftJoinRt(NodeInfoRtExample example);

    List<NodeInfoRt> selectNodeRtLeftJoinInfo(NodeRtInfoExample example);

    void batchUpdateNodeRt(List<NodeRt> nodeRtList);

    int insertNodeRt(NodeRt nodeRt);

    List<NodeRt> selectNodeRt(NodeRtExample example);

    NodeRt selectNodeRt(String nodeId);

    int updateNodeRt(NodeRt nodeRt);

    int updateNodeRtSelective(NodeRt nodeRt, NodeRtExample example);

    long countNodeRt(NodeRtExample example);

    boolean batchInsertNodeRt(Set<NodeRt> nodeRtList);

    // node_ins
    int insertNodeIns(NodeIns nodeIns);

    int updateNodeIns(NodeIns nodeIns);

    boolean batchUpdateNodeIns(List<NodeIns> nodeInsList);

    List<NodeIns> selectNodeIns(NodeInsExample example);

    int updateNodeInsSelective(NodeIns nodeIns, NodeInsExample example);

}
