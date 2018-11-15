package tbcloud.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.NodeConst;
import tbcloud.node.model.NodeInfo;
import tbcloud.node.model.NodeInfoExample;
import tbcloud.user.api.http.req.*;
import tbcloud.user.api.http.rsp.ImgCodeRsp;
import tbcloud.user.api.http.rsp.PageRsp;
import tbcloud.user.api.http.rsp.SignInRsp;
import tbcloud.user.model.UserImgCode;
import tbcloud.user.model.UserInfo;
import tbcloud.user.model.UserInfoExample;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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

    private int innerImgCodeId(String imgCodeId) {
        return Math.abs(imgCodeId.hashCode() % ApiConst.IMGCODE_MAX_ID) + 1;
    }

    @POST
    @Path("imgcode/get")
    public Result<ImgCodeRsp> getImgCode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version) {
        LOG.info("{} {}", ui.getPath(), version);
        Result<ImgCodeRsp> r = new Result<>();

        String imgCodeIdStr = IDUtil.genImgCodeId(version); //TODO ip
        UserImgCode imgCode = UserDao.selectUserImgCode(innerImgCodeId(imgCodeIdStr));

        if (imgCode == null) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("not found imgCode");
            return r;
        }

        ImgCodeRsp data = new ImgCodeRsp();
        data.setImgCodeId(imgCodeIdStr);
        data.setImgCodeUrl(String.join("/", Plugin.getConfig(ApiConst.QINIU_DOMAIN), imgCode.getImgPath()));
        r.setData(data);
        return r;
    }

    @POST
    @Path("vcode/get")
    public Result<String> getVcode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, MobileVcodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<String> r = new Result<>();

        // TODO check mobile 11 digital

        // validate imgCode
        int imgCodeId = innerImgCodeId(req.getImgCodeId());
        if (!isValidImgCode(imgCodeId, req.getImgCode())) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param imgCode:" + req.getImgCode());
            return r;
        }

        // TODO 重复发送

        String vcode = IDUtil.genMobileVcode(4);
        if (isDebug()) { //only debug mode
            r.setData(vcode);
        }

        // TODO aync to send to mobile
        String mobile = req.getMobile();

        // cache
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile, vcode, 60);

        LOG.info("send vcode {} to {}", vcode, req.getMobile());
        return r;
    }

    @POST
    @Path("signup/passwd")
    public Result<Void> passwdSignUp(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, SignUpPasswdReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<Void> r = new Result<>();

        String mobile = req.getMobile();
        String vcode = req.getVcode();

        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode) || StringUtil.isEmpty(req.getPasswd())) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // mobile must be not used  TODO opt
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList != null && userInfoList.size() > 0) {
            r.setCode(ApiCode.DB_INSERT_ERROR);
            r.setMsg("mobile existed " + mobile);
            return r;
        }

        // insert user
        UserInfo userInfo = new UserInfo();
        userInfo.setMobile(req.getMobile());
        //TODO userInfo.setInviteCode("");
        userInfo.setPasswd(StringUtil.MD5Encode(req.getPasswd() + ApiConst.USER_PASSWD_SALT_1));

        if (UserDao.insertUserInfo(userInfo) != 1) {
            r.setCode(ApiCode.DB_INSERT_ERROR);
            r.setMsg("signup unknow error " + mobile);
            return r;
        } else {
            LOG.info("create new userId:" + userInfo.getId());
        }

        // TODO async to update apikey
        String apikey = IDUtil.genApikeyV1(userInfo.getId());


        return r;
    }

    @POST
    @Path("signin/passwd")
    public Result<SignInRsp> passwdSignIn(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, SignInPasswdReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<SignInRsp> r = new Result<>();

        String mobile = req.getMobile();

        // TODO check mobile 11 digital

        // validate imgCode
        int imgCodeId = innerImgCodeId(req.getImgCodeId());
        if (!isValidImgCode(imgCodeId, req.getImgCode())) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param imgCode:" + req.getImgCode());
            return r;
        }

        // select user
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile)
                .andPasswdEqualTo(StringUtil.MD5Encode(req.getPasswd() + ApiConst.USER_PASSWD_SALT_1))
                .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList == null || userInfoList.size() != 1) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("check your passwd! not found user:" + mobile);
            return r;
        }

        UserInfo userInfo = userInfoList.get(0);
        String token = IDUtil.genToken(userInfo.getId());
        // save to redis
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + userInfo.getId(), token, ApiConst.REDIS_EXPIRED_24H);

        SignInRsp rsp = new SignInRsp();
        rsp.setToken(token);
        r.setData(rsp);
        return r;
    }

    @POST
    @Path("passwd/forget")
    public Result<Void> forgetPasswd(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, PasswdForgetReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<Void> r = new Result<>();

        String mobile = req.getMobile();
        String vcode = req.getVcode();
        String passwd = req.getPasswd();

        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode) || StringUtil.isEmpty(passwd)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // update passwd
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList == null || userInfoList.size() != 1) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("check your passwd! not found user:" + mobile);
            return r;
        }

        UserInfo userInfo = userInfoList.get(0);
        userInfo.setPasswd(StringUtil.MD5Encode(passwd + ApiConst.USER_PASSWD_SALT_1));
        if (UserDao.updateUserInfo(userInfo) != 1) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg("forgot passwd reset error. mobile:" + mobile);
            return r;
        }
        return r;
    }

    @POST
    @Path("passwd/reset")
    public Result<Void> resetPasswd(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, PasswdResetReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String passwd = req.getPasswd();

        UserInfo userInfo = reqContext.getUserInfo();

        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setId(userInfo.getId());
        newUserInfo.setPasswd(StringUtil.MD5Encode(passwd + ApiConst.USER_PASSWD_SALT_1));
        if (UserDao.updateUserInfo(newUserInfo) != 1) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg("forgot passwd reset error. userId:" + userInfo.getId());
            return r;
        }

        return r;
    }

    @POST
    @Path("info/get")
    public Result<UserInfo> getInfo(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfo> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        r.setData(reqContext.getUserInfo());
        return r;
    }

    @POST
    @Path("info/set")
    public Result<UserInfo> setInfo(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserInfoSetReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfo> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setId(userInfo.getId());
        newUserInfo.setName(req.getName());
        newUserInfo.setAvatar(req.getAvatar());
        UserDao.updateUserInfo(newUserInfo);

        return r;
    }

    @POST
    @Path("mobile/verify")
    public Result<UserInfo> verifyMobile(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, MobileVerifyReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfo> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String mobile = req.getMobile();
        String vcode = req.getVcode();

        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // update mobile
        UserInfo userInfo = reqContext.getUserInfo();
        String oldMobile = userInfo.getMobile();
        LOG.info("mobile verify from {} to {}", oldMobile, mobile);

        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setId(userInfo.getId());
        newUserInfo.setMobile(mobile);
        UserDao.updateUserInfo(newUserInfo); //TODO

        return r;
    }


    @POST
    @Path("email/modify")
    public Result<Void> modifyMobile(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        //TODO limit req

        // TODO generate token , save to redis, and async to send email
        String email = req.get("email");

        return r;
    }

    @GET
    @Path("email/verify")
    public void modifyMobile(@Context UriInfo ui, @QueryParam("token") String token) {
        LOG.info("{} {} ", ui.getPath(), token);
//        Result<Void> r = new Result<>();

        // TODO retrieve email info from redis,token maybe expired

        // TODO redirect
        try {
            Response.seeOther(new URI("")); //
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @POST
    @Path("node/bind")
    public Result<Void> bindNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.get("nodeId");
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) { //insert
            nodeInfo = new NodeInfo();
            nodeInfo.setNodeId(nodeId);
            nodeInfo.setBindTime(System.currentTimeMillis());
            nodeInfo.setIsBind(NodeConst.IS_BIND);
            nodeInfo.setUserId(userInfo.getId());
            if (NodeDao.insertNodeInfo(nodeInfo) != 1) {
                r.setCode(ApiCode.DB_INSERT_ERROR);
                r.setMsg(nodeId + " bind error!");
                return r;
            }
        } else {  //update
            //TODO check binded

            NodeInfo rebindNode = new NodeInfo();
            rebindNode.setNodeId(nodeId);
            rebindNode.setBindTime(System.currentTimeMillis());
            rebindNode.setIsBind(NodeConst.IS_BIND);
            rebindNode.setUserId(userInfo.getId());
            if (NodeDao.updateNodeInfo(rebindNode) != 1) {
                r.setCode(ApiCode.DB_UPDATE_ERROR);
                r.setMsg(nodeId + " bind error!");
                return r;
            }
        }

        return r;
    }

    /**
     * @param ui
     * @param version
     * @param token
     * @param req
     * @return
     */
    @POST
    @Path("node/bindbatch")
    public Result<Void> bindBatchNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }
        UserInfo userInfo = reqContext.getUserInfo();

        String fileType = req.get("fileType"); //TODO
        String file = req.get("file");
        if (StringUtil.isEmpty(fileType) || StringUtil.isEmpty(file)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss param");
            return r;
        }

        switch (fileType) { //TODO aysnc
            case "1": { //换行符
                String[] nodeIdList = file.split("\n", 1000);
                List<NodeInfo> nodeInfoList = new ArrayList<>(nodeIdList.length);
                long bindTime = System.currentTimeMillis();
                for (int i = 0; i < nodeIdList.length; ++i) {
                    NodeInfo nodeInfo = new NodeInfo();
                    nodeInfo.setNodeId(nodeIdList[i].trim());
                    nodeInfo.setBindTime(bindTime);
                    nodeInfo.setIsBind(NodeConst.IS_BIND);
                    nodeInfo.setUserId(userInfo.getId());

                    nodeInfoList.add(nodeInfo);
                }
                NodeDao.batchInsertNodeInfo(nodeInfoList);
                break;
            }
        }

        return r;
    }

    @POST
    @Path("node/unbind")
    public Result<Void> unbindNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.get("nodeId");
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + " not found");
            return r;
        }

        if (nodeInfo.getIsBind() == NodeConst.IS_UNBIND) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + "is unbinded");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("cann't not unbind others' node");
            return r;
        }

        NodeInfo unbindNode = new NodeInfo();
        unbindNode.setNodeId(nodeId);
        unbindNode.setIsBind(NodeConst.IS_UNBIND);
        unbindNode.setUnbindTime(System.currentTimeMillis());
        NodeDao.updateNodeInfo(unbindNode);

        return r;
    }

    @POST
    @Path("node/list")
    public Result<PageRsp<NodeInfo>> listNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, PageReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeInfo>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageCount = req.getPageCount();

        NodeInfoExample example = new NodeInfoExample();
        example.createCriteria().andUserIdEqualTo(userInfo.getId()).andIsBindEqualTo(NodeConst.IS_BIND).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("bind_time desc limit " + (pageNo - 1) * pageCount + "," + pageCount);
        List<NodeInfo> nodeList = NodeDao.selectNodeInfo(example);
        long count = NodeDao.countNodeInfo(example);

        // TODO firmwareUpgrade NodeRsp

        PageRsp<NodeInfo> data = new PageRsp<>();
        data.setCount(count);
        data.setPage(nodeList);
        r.setData(data);
        return r;
    }


    @POST
    @Path("node/share/join")
    public Result<Void> joinShareNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        String nodeId = req.get("nodeId");
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + " not found");
            return r;
        }

        if (nodeInfo.getIsBind() != NodeConst.IS_BIND) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + " is not binded");
            return r;
        }

        if (nodeInfo.getIsShare() == NodeConst.IS_SHARE) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + " is shared");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("cann't not unbind others' node");
            return r;
        }

        NodeInfo shareNode = new NodeInfo();
        shareNode.setNodeId(nodeId);
        shareNode.setIsShare(NodeConst.IS_SHARE);
        shareNode.setShareTime(System.currentTimeMillis());
        NodeDao.updateNodeInfo(shareNode);

        return r;
    }

    @POST
    @Path("node/share/quit")
    public Result<Void> quitShareNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        String nodeId = req.get("nodeId");
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + " not found");
            return r;
        }

        if (nodeInfo.getIsShare() == NodeConst.IS_UNSHARE) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + "is not shared");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("cann't not unbind others' node");
            return r;
        }

        NodeInfo shareNode = new NodeInfo();
        shareNode.setNodeId(nodeId);
        shareNode.setIsShare(NodeConst.IS_UNSHARE);
        shareNode.setUnshareTime(System.currentTimeMillis());
        NodeDao.updateNodeInfo(shareNode);
        return r;
    }


    @POST
    @Path("quit")
    public Result<Void> quit(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        // rm token
        deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + userInfo.getId());

        //TODO user_online

        return r;
    }
}