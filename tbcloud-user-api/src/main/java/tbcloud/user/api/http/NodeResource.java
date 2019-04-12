package tbcloud.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author dzh
 * @date 2019-04-11 15:22
 */
@Path("node")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NodeResource extends BaseResource {

    static final Logger LOG = LoggerFactory.getLogger(NodeResource.class);


}
