package tbcloud.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.elastic.model.client.PageResult;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.user.api.http.req.HttpProxyRecordReq;
import tbcloud.user.api.http.req.ReqContext;
import tbcloud.user.api.http.rsp.PageRsp;
import tbcloud.user.model.UserInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author dzh
 * @date 2019-01-29 00:01
 */
@Path("open/httpproxy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OpenHttpProxyResource extends OpenUserResource {

    static final Logger LOG = LoggerFactory.getLogger(OpenHttpProxyResource.class);

    @POST
    @Path("node/list")
    public Result<PageRsp<HttpProxyRecord>> listNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, HttpProxyRecordReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<HttpProxyRecord>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        PageRsp<HttpProxyRecord> data = new PageRsp<>();
        if (StringUtil.isEmpty(req.getId())) {
            Integer pageNo = req.getPageNo();
            Integer pageSize = req.getPageSize();

            if (req.getStartTime() == null || req.getEndTime() == null) {
                r.setCode(ApiCode.HTTP_MISS_PARAM);
                r.setMsg("miss time param");
                return r;
            }

            try {
                PageResult<HttpProxyRecord> result = Plugin.elastic().recordList(userInfo.getId(), pageNo, pageSize, req.getStartTime(), req.getEndTime(), null, null);
                data.setTotal(result.getTotal());
                data.setPage(result.getPage());
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                r.setCode(ApiCode.ELASTIC_SEARCH_IO);
            }
        } else {
            try {
                HttpProxyRecord record = Plugin.elastic().recordGet(userInfo.getId(), req.getId());
                data.setTotal(1);
                data.setPage(Arrays.asList(record));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                r.setCode(ApiCode.ELASTIC_SEARCH_IO);
            }
        }
        r.setData(data);

        return r;
    }
}
