package tbcloud.timer.mutex;

import jframe.core.plugin.annotation.InjectPlugin;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.jedis.service.JedisService;
import jframe.zk.service.CuratorService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import tbcloud.common.dao.service.CommonDaoService;
import tbcloud.httpproxy.dao.service.HttpProxyDaoService;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.dao.service.NodeDaoService;
import tbcloud.timer.TimerPlugin;
import tbcloud.user.dao.service.UserDaoService;

import java.lang.reflect.Type;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author dzh
 * @date 2018-12-13 10:35
 */
@Injector
public abstract class MutexTimer implements AutoCloseable {

    static Logger LOG = LoggerFactory.getLogger(MutexTimer.class);

    private LeaderSelector selector;

    private volatile boolean closed = false;

    @InjectPlugin
    static TimerPlugin plugin;

    @InjectService(id = "jframe.service.jedis")
    static JedisService Jedis;

    @InjectService(id = "jframe.service.zk.curator")
    static CuratorService Curator;

    @InjectService(id = "tbcloud.service.node.dao")
    static NodeDaoService NodeDao;

    @InjectService(id = "tbcloud.service.user.dao")
    static UserDaoService UserDao;

    @InjectService(id = "tbcloud.service.common.dao")
    static CommonDaoService CommonDao;

    @InjectService(id = "tbcloud.service.httpproxy.dao")
    static HttpProxyDaoService HttpProxyDao;

    public void start() {
        closed = false;
        LOG.info("{} start", name());

        LeaderSelectorListener listener = new LeaderSelectorListenerAdapter() {
            public void takeLeadership(CuratorFramework client) throws Exception {
                LOG.info("{} takeLeadership", name());
                long delayMs = delayMs();
                if (delayMs < 1) {
                    delayMs = 10 * 1000; //10s
                }
                ScheduledExecutorService timer = null;
                try {
                    timer = Executors.newScheduledThreadPool(1);
                    timer.scheduleWithFixedDelay(handle(), delayMs, delayMs, TimeUnit.MILLISECONDS);
                    while (true) {
                        if (closed) break;

                        if (client.getState() != CuratorFrameworkState.STARTED) {
                            LOG.info("zk is not started {}", client.getState());
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    if (timer != null) {
                        timer.shutdown();
                        timer.awaitTermination(10, TimeUnit.SECONDS);
                    }
                }
                LOG.info("{} quitLeadership", name());
            }
        };

        CuratorFramework zk = zk();
        if (zk == null) {
            LOG.error("{} failed to get zk conn", name());
            return;
        }
        selector = new LeaderSelector(zk, path(), listener);
        selector.autoRequeue();
        selector.start();
    }

    protected CuratorFramework zk() {
        return Curator.client(ApiConst.ZK_ID_TIMER);
    }

    protected Runnable handle() {
        return () -> {
            long ts = System.currentTimeMillis();
            int count = 0;
            try {
                count = doTimer();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            LOG.info("timer {} finish {} {}", name(), count, System.currentTimeMillis() - ts);
        };
    }

    protected abstract int doTimer();

    /**
     * the path for this leadership group
     *
     * @return zk path
     */
    protected abstract String path();

    /**
     * 间隔执行时间 ms
     *
     * @return
     */
    protected abstract long delayMs();

    /**
     * unique timer name
     *
     * @return
     */
    protected String name() {
        return getClass().getSimpleName();
    }

    @Override
    public void close() throws Exception {
        closed = true;
        if (selector != null) selector.close();
        LOG.info("{} closed", name());
    }

    protected String getFromRedis(String id, String key) {
        try (redis.clients.jedis.Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                return jedis.get(key);
            }
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Type type) {
        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                String json = jedis.get(key);
                if (!StringUtil.isEmpty(json)) {
                    return GsonUtil.<T>fromJson(json, type);
                }
            }
        }
        return null;
    }

    protected <T> T getFromRedis(String id, String key, Class<T> clazz) {
        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                String json = jedis.get(key);
                if (!StringUtil.isEmpty(json)) {
                    return GsonUtil.<T>fromJson(json, clazz);
                }
            }
        }
        return null;
    }

    public void setToRedis(String id, String key, String value, Integer expiredSeconds) {
        if (value == null) return;

        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                jedis.setex(key, expiredSeconds, value);
            }
        }
    }

    public void saddToRedis(String id, String key, String... value) {
        if (value == null) return;

        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                jedis.sadd(key, value);
            }
        }
        return;
    }

    public void deleteFromRedis(String id, String key) {
        if (StringUtil.isEmpty(key)) return;

        try (Jedis jedis = Jedis.getJedis(id)) {
            if (jedis != null) {
                jedis.del(key);
            }
        }
    }
}
