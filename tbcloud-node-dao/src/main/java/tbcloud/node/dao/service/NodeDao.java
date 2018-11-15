package tbcloud.node.dao.service;

import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeInfoExample;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-12 17:16
 */
public interface NodeDao {

    int insertNodeInfo(NodeInfo nodeInfo);

    void batchInsertNodeInfo(List<NodeInfo> nodeInfoList);

    List<NodeInfo> selectNodeInfo(NodeInfoExample example);

    NodeInfo selectNodeInfo(String nodeId);

    int updateNodeInfo(NodeInfo nodeInfo);

    long countNodeInfo(NodeInfoExample example);
}
