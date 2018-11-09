package tbcloud.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.Result;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.lang.management.ManagementFactory;

/**
 * @author dzh
 */
@Path("test")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TestResource extends BaseResource {

    static Logger LOG = LoggerFactory.getLogger(TestResource.class);

    @GET
    @Path("ping")
    public Result<String> ping(@Context UriInfo ui) {
        LOG.info("{}", ui.getPath());
        Result<String> r = new Result<>();
        r.setData(ManagementFactory.getRuntimeMXBean().getName() + " " + System.currentTimeMillis());
        return r;
    }

}
