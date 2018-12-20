package tbcloud.node.job;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSenderRecver;
import jframe.core.plugin.annotation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.node.job.impl.JoinHttpProxyJob;
import tbcloud.node.job.impl.QuitHttpProxyJob;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dzh
 * @date 2018-12-13 13:54
 */
@Message(isSender = true, isRecver = true, msgTypes = {MsgType.NODE_ONLINE, MsgType.NODE_OFFLINE, MsgType.NODE_JOIN_HTTPPROXY, MsgType.NODE_QUIT_HTTPPROXY})
public class NodeJobPlugin extends PluginSenderRecver {

    static Logger LOG = LoggerFactory.getLogger(NodeJobPlugin.class);

    private Map<Integer, NodeJob> jobs = new HashMap<>();

    @Override
    public void start() throws PluginException {
        super.start();
        startJobs();
    }

    private void startJobs() {
        startJob(JoinHttpProxyJob.class);
        startJob(QuitHttpProxyJob.class);
    }

    void startJob(Class<? extends NodeJob> clazz) {
        try {
            NodeJob job = clazz.getConstructor().newInstance();
            job.plugin(this);
            job.start();
            jobs.put(job.msgType(), job);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }


    @Override
    protected void doRecvMsg(Msg<?> msg) {

    }

    @Override
    protected boolean canRecvMsg(Msg<?> msg) {
        return true;
    }

    @Override
    public void stop() throws PluginException {
        super.stop();
        stopJobs();
    }

    private void stopJobs() {
        jobs.forEach((type, job) -> {
            try {
                job.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }
}
