package tbcloud.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.elastic.model.client.PageResult;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.util.IDUtil;
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
public class OpenHttpProxyResource extends BaseResource {

    static final Logger LOG = LoggerFactory.getLogger(OpenHttpProxyResource.class);

    @POST
    @Path("record/list")
    public Result<PageRsp<HttpProxyRecord>> listRecord(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, HttpProxyRecordReq req) {
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

            long st = System.currentTimeMillis();
            if (req.getStartTime() == null) {
                req.setStartTime(st - 24 * 3600000);
            }
            if (req.getEndTime() == null) {
                req.setEndTime(st);
            }

            if (req.getEndTime() - req.getStartTime() < 0) {
                r.setCode(ApiCode.INVALID_PARAM);
                r.setMsg("endTime - startTime < 0");
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

    private final int validateToken(ReqContext reqContext) {
        String token = reqContext.getToken();

        long usrId = IDUtil.decodeUserIDFromToken(token);
        if (usrId <= 0) return ApiCode.TOKEN_INVALID;

        UserInfo userInfo = UserDao.selectUserInfo(usrId);
        if (userInfo == null) return ApiCode.TOKEN_INVALID;
        reqContext.setUserInfo(userInfo); // update context

        if (Plugin.envName().equals(ApiConst.ENV_DEV)) return ApiCode.SUCC;  // 方便测试
        String usrToken = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_OPEN_TOKEN_ + usrId);
        if (usrToken == null) return ApiCode.TOKEN_EXPIRED;
        if (!usrToken.equals(token)) return ApiCode.TOKEN_INVALID;

        return ApiCode.SUCC;
    }
}
