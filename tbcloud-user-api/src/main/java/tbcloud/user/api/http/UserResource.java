package tbcloud.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * @author dzh
 * @date 2018-11-08 20:20
 */

@Path("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource extends BaseResource {

    static Logger LOG = LoggerFactory.getLogger(UserResource.class);

    @POST
    @Path("imgcode/get")
    public Result<Map<String, String>> getImgCode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version) {
        LOG.info("{} {}", ui.getPath(), version);
        Result<Map<String, String>> r = new Result<>();

        return null;
    }

}
