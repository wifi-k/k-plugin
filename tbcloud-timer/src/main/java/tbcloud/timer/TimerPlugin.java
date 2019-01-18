package tbcloud.timer;

import jframe.core.plugin.DefPlugin;
import jframe.core.plugin.PluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.timer.mutex.*;

import java.util.LinkedList;
import java.util.List;

/**
 * @author dzh
 * @date 2018-12-12 12:15
 */
public class TimerPlugin extends DefPlugin {

    static Logger LOG = LoggerFactory.getLogger(TimerPlugin.class);

    private List<MutexTimer> timers = new LinkedList<>();

    @Override
    public void start() throws PluginException {
        super.start();

        startMutexTimer();
    }

    private void startMutexTimer() {
        startTimer(HttpProxyOnlineAdd.class);
        startTimer(IpInfoFind.class);
        startTimer(NodeInsFailed.class);
        startTimer(NodeInsRetry.class);

        startTimer(NodeOffline.class);
        startTimer(UserOffline.class);
    }

    private void startTimer(Class<? extends MutexTimer> clazz) {
        try {
            MutexTimer t = clazz.getConstructor().newInstance();
            t.start();
            timers.add(t);
            LOG.info("start timer {}", clazz.getName());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void stop() throws PluginException {
        super.stop();

        closeMutexTimer();
    }

    private void closeMutexTimer() {
        timers.forEach(t -> {
            try {
                t.close();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }


//    @Override
//    protected void doRecvMsg(Msg<?> msg) {
//    }
//    @Override
//    protected boolean canRecvMsg(Msg<?> msg) {
//        return false;
//    }
}
