package tbcloud.user.job;

import jframe.core.msg.Msg;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.dao.service.NodeDaoService;
import tbcloud.user.dao.service.UserDaoService;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author dzh
 * @date 2018-11-19 16:38
 */
@Injector
public abstract class UserJob extends AbstractJob {

    protected static Logger LOG = LoggerFactory.getLogger(UserJob.class);

    UserJobPlugin plugin;

    ExecutorService threads;

    public abstract int msgType();

    private volatile boolean closed = false;

    @InjectService(id = "jframe.service.jedis")
    protected static JedisService Jedis;

    @InjectService(id = "tbcloud.service.user.dao")
    protected static UserDaoService UserDao;

    @InjectService(id = "tbcloud.service.node.dao")
    protected static NodeDaoService NodeDao;

    public UserJob() {

    }

    void plugin(UserJobPlugin plugin) {
        this.plugin = plugin;
    }

    protected UserDaoService dao() {
        return UserDao;
    }

    protected UserJobPlugin plugin() {
        return plugin;
    }

    public void start() {
        // TODO rejected handle
        // TODO threadpool is too simple
        threads = new ThreadPoolExecutor(1, threadSize(), 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000));
        LOG.info("start job msgType-{}", msgType());
    }

    protected int threadSize() {
        int defThreads = Runtime.getRuntime().availableProcessors();
        try {
            String jobThreadSize = Optional.ofNullable(plugin().getConfig(id() + ".threadpool.size")).orElse(
                    plugin().getConfig(ApiConst.JOB_THREADPOOL_SIZE, String.valueOf(defThreads))
            );
            int th = Integer.parseInt(jobThreadSize);
            return th > 0 ? th : defThreads;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return defThreads;
    }

    protected ExecutorService threads() {
        return this.threads;
    }

    protected abstract void doJob(Msg<?> msg) throws Exception;

    public boolean offer(final Msg<?> msg) {
        if (closed) return false;
        try {
            threads.submit(() -> {
                long ts = System.currentTimeMillis();

                try {
                    UserJob.this.doJob(msg);
                } catch (Exception e) {
                    catchException(e, msg);
                }
                LOG.info("{} msg {}", System.currentTimeMillis() - ts, GsonUtil.toJson(msg));
            });
            return true;
        } catch (Exception e) {
            catchException(e, msg);
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        closed = true;
        try {
            threads.shutdown();
            threads.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage(), e);
        }
        LOG.info("close job msgType-{}", msgType());
    }

    protected void catchException(Exception e, Msg<?> msg) {
        LOG.error("submit miss-{}", GsonUtil.toJson(msg));
        LOG.error(e.getMessage(), e);
    }

    public void deleteFromRedis(String id, String key) {
        if (StringUtil.isEmpty(key)) return;

        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                jedis.del(key);
            }
        }
    }

    @Override
    protected String id() {
        return getClass().getSimpleName();
    }


}
