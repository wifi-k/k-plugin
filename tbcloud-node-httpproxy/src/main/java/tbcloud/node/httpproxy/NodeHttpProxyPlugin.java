package tbcloud.node.httpproxy;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginSenderRecver;
import jframe.core.plugin.annotation.Plugin;

/**
 * @author dzh
 * @date 2018-11-26 01:09
 */
@Plugin(startOrder = -1)
public class NodeHttpProxyPlugin extends PluginSenderRecver {

    @Override
    protected void doRecvMsg(Msg<?> msg) {

    }

    @Override
    protected boolean canRecvMsg(Msg<?> msg) {
        return false;
    }
}
