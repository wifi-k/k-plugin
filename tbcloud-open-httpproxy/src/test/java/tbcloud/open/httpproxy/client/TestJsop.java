package tbcloud.open.httpproxy.client;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @author dzh
 * @date 2018-12-29 13:46
 */
public class TestJsop {

    static Logger LOG = LoggerFactory.getLogger(TestJsop.class);

    @Test
    public void testUri() {
        URI uri = URI.create("https://test.user.famwifi.com/api/test/ping?key=12&key2=13");
        LOG.info("{}://{}:{}{}?{}", uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery());

    }

}
