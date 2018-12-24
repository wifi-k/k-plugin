package tbcloud.user.api;

import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiConst;
import tbcloud.user.api.http.OpenUserResource;
import tbcloud.user.api.http.TestResource;
import tbcloud.user.api.http.UserResource;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author dzh
 */
class ApiHttpServer extends Application {

    static Logger LOG = LoggerFactory.getLogger(ApiHttpServer.class);

    UserApiPlugin plugin;

    private NettyJaxrsServer netty;

    ApiHttpServer(UserApiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(TestResource.class);
        // user
        classes.add(UserResource.class);
        // open
        classes.add(OpenUserResource.class);

        return classes;
    }

    public void startHttpServer() {
        try {
            int port = Integer.parseInt(plugin.getConfig(ApiConst.HTTP_PORT, "80"));
            String host = plugin.getConfig(ApiConst.HTTP_HOST, "0.0.0.0");
            int ioCount = Integer.parseInt(plugin.getConfig(ApiConst.HTTP_IO_THREADS, "-1"));
            ioCount = ioCount < 0 ? Runtime.getRuntime().availableProcessors() : ioCount;
            int workCount = Integer.parseInt(plugin.getConfig(ApiConst.HTTP_WORK_THREADS, "100"));

            LOG.info("Starting http server, listen on {}:{}", host, port);
            netty = new NettyJaxrsServer();
            netty.setIoWorkerCount(ioCount);
            netty.setExecutorThreadCount(workCount);

            ResteasyDeployment deployment = new ResteasyDeployment();
            deployment.setProviderFactory(new ResteasyProviderFactory());
            // deployment.getProviderFactory().register(ResteasyJacksonProvider.class);
            deployment.setApplication(this);
            netty.setDeployment(deployment);
            netty.setHostname(host);
            netty.setPort(port);
            netty.setRootResourcePath(plugin.getConfig(ApiConst.HTTP_ROOT, "/"));

            // netty.setSecurityDomain(null);
            // if (isHttpsEnabled()) {
            // SelfSignedCertificate ssc = new SelfSignedCertificate();
            // netty.setSSLContext(SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build());
            // }

            netty.start();
            LOG.info("Start http server successfully!");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * default value is false
     *
     * @return
     */
    // private boolean isHttpsEnabled() {
    // try {
    // return Boolean.parseBoolean(plugin.getConfig(ApiConst.HTTPS_ENABLED, "false"));
    // } catch (Exception e) {
    // LOG.error(e.getMessage());
    // }
    // return false;
    // }
    public void stopHttpServer() {
        if (netty != null) {
            netty.stop();
        }
        LOG.info("Stop http server successfully!");
    }

}
