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


    int insertNodeIns(NodeIns nodeIns);

    int updateNodeIns(NodeIns nodeIns);

    boolean batchUpdateNodeIns(List<NodeIns> nodeInsList);

    List<NodeIns> selectNodeIns(NodeInsExample example);

    int updateNodeInsSelective(NodeIns nodeIns, NodeInsExample example);

}
