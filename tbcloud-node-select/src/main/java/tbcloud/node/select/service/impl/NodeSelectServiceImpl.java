package tbcloud.node.select.service.impl;

import jframe.core.plugin.annotation.Injector;
import jframe.core.plugin.annotation.Start;
import jframe.core.plugin.annotation.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.AppEnum;
import tbcloud.node.select.service.NodeSelectService;

import java.util.Collections;
import java.util.List;

/**
 * @author dzh
 * @date 2018-12-20 19:39
 */
@Injector
public class NodeSelectServiceImpl implements NodeSelectService {

    static Logger LOG = LoggerFactory.getLogger(NodeSelectServiceImpl.class);

    private HttpProxyNodeSelector httpProxySelector;

    @Start
    void start() {
        LOG.info("NodeSelectService start");
        httpProxySelector = new HttpProxyNodeSelector();
    }

    @Stop
    void stop() {
        LOG.info("NodeSelectService stop");
    }

    @Override
    public List<String> randomOnline(String appId, int count) {
        if (AppEnum.HTTP_PROXY.getId().equals(appId)) {
            return httpProxySelector.randomOnline(count);
        }
        return Collections.emptyList();
    }
}
