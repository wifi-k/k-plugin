package tbcloud.user.api.http;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 共享计算
 *
 * @author dzh
 * @date 2018-11-08 20:21
 */
@Path("share")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ShareResource extends BaseResource {

}
