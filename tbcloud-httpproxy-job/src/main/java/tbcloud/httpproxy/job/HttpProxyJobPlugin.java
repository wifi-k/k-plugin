package tbcloud.httpproxy.job;

import jframe.core.msg.Msg;
import jframe.core.plugin.PluginException;
import jframe.core.plugin.PluginSenderRecver;
import jframe.core.plugin.annotation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.elastic.model.client.TbcloudESClient;
import tbcloud.httpproxy.job.impl.AddHttpProxyRecordJob;
import tbcloud.httpproxy.job.impl.JoinHttpProxyJob;
import tbcloud.httpproxy.job.impl.QuitHttpProxyJob;
import tbcloud.lib.api.msg.MsgType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dzh
 * @date 2019-01-21 17:31
 */
@Message(isSender = true, isRecver = true, msgTypes = {MsgType.NODE_JOIN_HTTPPROXY, MsgType.NODE_QUIT_HTTPPROXY, MsgType.HTTPPROXY_RECORD_ADD})
public class HttpProxyJobPlugin extends PluginSenderRecver {

    static Logger LOG = LoggerFactory.getLogger(HttpProxyJobPlugin.class);

    private Map<Integer, HttpProxyJob> jobs = new HashMap<>();

    private TbcloudESClient elastic;

    @Override
    public void start() throws PluginException {
        super.start();

        try {
            elastic = new TbcloudESClient(getConfig("file.es"));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        startJobs();
    }

    private void startJobs() {
        startJob(JoinHttpProxyJob.class);
        startJob(QuitHttpProxyJob.class);
        startJob(AddHttpProxyRecordJob.class);
    }

    public TbcloudESClient elastic() {
        return elastic;
    }

    void startJob(Class<? extends HttpProxyJob> clazz) {
        try {
            HttpProxyJob job = clazz.getConstructor().newInstance();
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
        HttpProxyJob job = jobs.get(type);
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
        if (elastic != null) elastic.close();
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
