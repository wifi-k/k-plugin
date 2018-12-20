package tbcloud.node.select.service;

import jframe.core.plugin.annotation.Service;

import java.util.List;

/**
 * 选择节点
 *
 * @author dzh
 * @date 2018-12-20 19:34
 */
@Service(clazz = "tbcloud.node.select.service.impl.NodeSelectServiceImpl", id = "tbcloud.service.node.select")
public interface NodeSelectService {

    /**
     * 随机选择count个在线节点
     *
     * @param appId {@link tbcloud.lib.api.AppEnum}
     * @param count 最多返回数量
     * @return
     */
    List<String> randomOnline(String appId, int count);


}
