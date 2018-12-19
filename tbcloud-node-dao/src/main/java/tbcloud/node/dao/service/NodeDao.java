package tbcloud.node.dao.service;

import tbcloud.node.model.*;
import tbcloud.node.model.ext.NodeInfoRt;
import tbcloud.node.model.ext.NodeInfoRtExample;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-12 17:16
 */
public interface NodeDao {

    NodeApp selectNodeApp(long id);

    int insertNodeApp(NodeApp nodeApp);

    int updateNodeApp(NodeApp nodeApp);

    int updateNodeAppSelective(NodeApp nodeApp, NodeAppExample example);

    int insertNodeInfo(NodeInfo nodeInfo);

    void batchInsertNodeInfo(List<NodeInfo> nodeInfoList);

    List<NodeInfo> selectNodeInfo(NodeInfoExample example);

    NodeInfo selectNodeInfo(String nodeId);

    int updateNodeInfo(NodeInfo nodeInfo);

    long countNodeInfo(NodeInfoExample example);

    List<NodeInfoRt> selectNodeInfoLeftJoinRt(NodeInfoRtExample example);

    List<NodeInfoRt> selectNodeRtLeftJoinInfo(NodeRtInfoExample example);

    int insertNodeRt(NodeRt nodeRt);

    List<NodeRt> selectNodeRt(NodeRtExample example);

    NodeRt selectNodeRt(String nodeId);

    int updateNodeRt(NodeRt nodeRt);

    int updateNodeRtSelective(NodeRt nodeRt, NodeRtExample example);

    long countNodeRt(NodeRtExample example);

    void batchInsertNodeRt(List<NodeRt> nodeRtList);


    int insertNodeIns(NodeIns nodeIns);

    int updateNodeIns(NodeIns nodeIns);

    List<NodeIns> selectNodeIns(NodeInsExample example);

    int updateNodeInsSelective(NodeIns nodeIns, NodeInsExample example);

}
