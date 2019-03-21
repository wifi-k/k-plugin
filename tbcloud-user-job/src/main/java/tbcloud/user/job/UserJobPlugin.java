package tbcloud.user.job;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSenderRecver;
import jframe.core.plugin.annotation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.user.job.impl.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dzh
 * @date 2018-11-19 16:32
 */
@Message(isSender = true, isRecver = true, msgTypes = {MsgType.LOGIN_OUT, MsgType.EMAIL_MODIFY, MsgType.MOBILE_VCODE,
        MsgType.NODE_JOIN_SHARE, MsgType.NODE_QUIT_SHARE, MsgType.DELETE_QINIU_OBJECT, MsgType.NODE_FIRMWARE_UPGRADE})
public class UserJobPlugin extends PluginSenderRecver {

    static Logger LOG = LoggerFactory.getLogger(UserJobPlugin.class);

    private Map<Integer, UserJob> jobs = new HashMap<>();

    @Override
    public void start() throws PluginException {
        super.start();
        startJobs();
    }

    private void startJobs() { // TODO conf
        startJob(EmailModifyJob.class);
        startJob(MobileVCodeJob.class);
        startJob(NodeJoinShareJob.class);
        startJob(NodeQuitShareJob.class);
        startJob(UserLoginJob.class);
        startJob(QiniuDeleteJob.class);
        startJob(NodeUpgradeJob.class);
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
        LOG.info("recv msg {}", msg);
        int type = msg.getType();
        UserJob job = jobs.get(type);
        if (job == null) {
            LOG.error("not found Job of msgType-{}", type);
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
        jobs.forEach((type, job) -> {
            try {
                job.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }
}
