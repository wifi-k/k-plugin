package tbcloud.user.job;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSenderRecver;
import jframe.core.plugin.annotation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.user.job.impl.EmailModifyJob;
import tbcloud.user.job.impl.MobileVCodeJob;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dzh
 * @date 2018-11-19 16:32
 */
@Message(isSender = true, isRecver = true, msgTypes = {})
public class UserJobPlugin extends PluginSenderRecver {

    static Logger LOG = LoggerFactory.getLogger(UserJobPlugin.class);

    private Map<Integer, UserJob> jobs = new HashMap<>();

    @Override
    public void start() throws PluginException {
        super.start();
        startJobs();
    }

    private void startJobs() {
        startJob(MobileVCodeJob.class);
        startJob(EmailModifyJob.class);
    }

    void startJob(Class<? extends UserJob> clazz) {
        try {
            UserJob job = clazz.getConstructor().newInstance();
            job.plugin(this);
            job.start();
            jobs.put(job.msgType(), job);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doRecvMsg(Msg<?> msg) {
        LOG.info("recv msg-{}", msg);
        int type = msg.getType();
        UserJob job = jobs.get(type);
        if (job == null) {
            LOG.error("not found ScanJob of this msgType-{}", type);
            LOG.error("discard msg-{}", msg);
            return;
        }
        boolean succ = job.offer(msg);
        if (!succ) {
            LOG.error("discard msg-{}", msg);
        }
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
        for (UserJob j : jobs.values()) {
            try {
                j.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
