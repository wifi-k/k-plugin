package tbcloud.user.api.http;

import jframe.core.msg.PluginMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.ApiField;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.msg.EmailModify;
import tbcloud.lib.api.msg.MobileVCode;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.msg.UserLogin;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.*;
import tbcloud.node.model.ext.NodeInfoRt;
import tbcloud.user.api.http.req.*;
import tbcloud.user.api.http.rsp.ImgCodeRsp;
import tbcloud.user.api.http.rsp.PageRsp;
import tbcloud.user.api.http.rsp.ShareSumRsp;
import tbcloud.user.api.http.rsp.SignInRsp;
import tbcloud.user.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * 用户平台
 *
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
    public Result<ImgCodeRsp> getImgCode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version) {
        LOG.info("{} {}", ui.getPath(), version);
        Result<ImgCodeRsp> r = new Result<>();

        String imgCodeId = IDUtil.genImgCodeId(version); //TODO ip
        UserImgCode imgCode = UserDao.selectUserImgCode(IDUtil.innerImgCodeId(imgCodeId));

        if (imgCode == null) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("not found imgCode");
            return r;
        }

        ImgCodeRsp data = new ImgCodeRsp();
        data.setImgCodeId(imgCodeId);
        data.setImgCodeUrl(String.join("/", Plugin.getConfig(ApiConst.QINIU_DOMAIN), imgCode.getImgPath()));
        r.setData(data);
        return r;
    }

    @POST
    @Path("vcode/get")
    public Result<String> getVCode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, MobileVcodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<String> r = new Result<>();

        // validate imgCode
        if (!isValidImgCode(req.getImgCodeId(), req.getImgCode())) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param imgCode " + req.getImgCode());
            return r;
        }

        String mobile = req.getMobile();
        if (StringUtil.isEmpty(mobile) || mobile.length() != 11) {//TODO 国际化
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid mobile " + mobile);
            return r;
        }

        String vcode = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile);
        if (!StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.OP_MORE_THAN_LIMIT);
            r.setMsg("request more than limit");
            return r;
        }

        vcode = IDUtil.genMobileVcode(4);
        if (Plugin.isDebug() && !Plugin.envName().equals(ApiConst.ENV_ONLINE)) {
            r.setData(vcode);
            r.setMsg("测试时返回");
        }

        if (!ApiConst.ENV_DEV.equals(Plugin.envName())) { // not send if dev
            MobileVCode msg = new MobileVCode();
            msg.setType(req.getType());
            msg.setMobile(mobile);
            msg.setCode(vcode);
            Plugin.send(new PluginMsg<MobileVCode>().setType(MsgType.MOBILE_VCODE).setValue(msg));
        }

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
        userInfo.setName(req.getName());
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
        if (!isValidImgCode(req.getImgCodeId(), req.getImgCode())) {
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

        // send UserLogin
        UserLogin msg = new UserLogin();
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_ONLINE);
        msg.setToken(token);
        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

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


    /**
     * 请求修改email,将异步发送确认邮件到新邮箱。通过用verifyEmail认证后修改成功
     *
     * @param ui
     * @param version
     * @param token
     * @param req
     * @return
     */
    @POST
    @Path("email/modify")
    public Result<String> modifyEmail(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<String> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        // TODO 验证必要参数

        UserInfo userInfo = reqContext.getUserInfo();
        String name = userInfo.getName();


        String email = req.get(ApiField.F_email);
        String emailToken = IDUtil.genEmailToken(userInfo.getId());

        if (Plugin.isDebug() && !Plugin.envName().equals(ApiConst.ENV_ONLINE)) {
            r.setData(emailToken);
            r.setMsg("测试时返回");
        }

        // email saved into redis
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_EMAIL_TOKEN_ + emailToken, email, ApiConst.REDIS_EXPIRED_1H);

        // async to send email
        EmailModify em = new EmailModify();
        em.setEmail(email);
        em.setName(Optional.ofNullable(name).orElse(userInfo.getMobile()));
        em.setToken(emailToken);

        Plugin.send(new PluginMsg<EmailModify>().setType(MsgType.EMAIL_MODIFY).setValue(em));
        return r;
    }

    @POST
    @Path("email/verify")
    public Result<Void> verifyEmail(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, req);
        Result<Void> r = new Result<>();

        String emailToken = req.get(ApiField.F_token);
        String passwd = req.get(ApiField.F_passwd);
        if (StringUtil.isEmpty(emailToken) || StringUtil.isEmpty(passwd)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        //TODO limit req

        // retrieve email info from redis,token maybe expired
        String email = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_EMAIL_TOKEN_ + emailToken);
        if (StringUtil.isEmpty(email)) {
            r.setCode(ApiCode.TOKEN_EXPIRED);
            r.setMsg("邮件链接已经超过有效期，请重新发起邮件修改请求");
            return r;
        }

        long userId = IDUtil.decodeUserIDFromEmailToken(emailToken);
        UserInfo userInfo = UserDao.selectUserInfo(userId);
        if (userInfo == null) {
            r.setCode(ApiCode.TOKEN_INVALID);
            r.setMsg("邮件链接包含无效的参数，请联系树熊云");
            return r;
        }

        // TODO 最多允许输入3次
        passwd = StringUtil.MD5Encode(passwd + ApiConst.USER_PASSWD_SALT_1);
        if (!passwd.equals(userInfo.getPasswd())) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("请输入正确的用户密码");
            return r;
        }

        // update email
        UserInfo updateEmail = new UserInfo();
        updateEmail.setId(userId);
        updateEmail.setEmail(email);
        UserDao.updateUserInfo(updateEmail);

//        try {
//            Response.seeOther(new URI("")); //
//        } catch (URISyntaxException e) {
//            LOG.error(e.getMessage(), e);
//        }
        return r;
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

        String nodeId = req.get(ApiField.F_nodeId);
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
            Long ownUserId = nodeInfo.getUserId();
            if (ownUserId != null && ownUserId != userInfo.getId()) {
                r.setCode(ApiCode.USR_INVALID);
                r.setMsg(nodeId + " be bound!");
                return r;
            }

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

        // upsert node_rt
        NodeRt nodeRt = NodeDao.selectNodeRt(nodeId);
        if (nodeRt == null) {
            nodeRt = new NodeRt();
            nodeRt.setNodeId(nodeId);
            //nodeRt.setStatus(NodeConst.STATUS_OFFLINE);
            nodeRt.setUserId(userInfo.getId());
            NodeDao.insertNodeRt(nodeRt);
        } else {
            NodeRt rebindNode = new NodeRt();
            rebindNode.setNodeId(nodeId);
            rebindNode.setUserId(userInfo.getId());
            //rebindNode.setStatus(NodeConst.STATUS_OFFLINE);
            NodeDao.updateNodeRt(rebindNode);
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

        // TODO
        String fileType = req.get(ApiField.F_fileType);
        String file = req.get(ApiField.F_file);
        if (StringUtil.isEmpty(fileType) || StringUtil.isEmpty(file)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss param");
            return r;
        }

        switch (fileType) { //TODO aysnc to insert or limit req
            case "1": { //换行符
                String[] nodeIdList = file.split("[\r\n]+");
                if (nodeIdList.length < 1 || nodeIdList.length > 1000) { //TODO config
                    r.setCode(ApiCode.INVALID_PARAM);
                    r.setMsg("nodes must be less than 1000");
                    return r;
                }

                // 去重
                Set<NodeInfo> nodeInfoList = new HashSet<>(nodeIdList.length);
                Set<NodeRt> nodeRtList = new HashSet<>(nodeIdList.length);

                long bindTime = System.currentTimeMillis();
                for (int i = 0; i < nodeIdList.length; ++i) {
                    String nodeId = nodeIdList[i].trim();
                    if (StringUtil.isEmpty(nodeId)) continue; //skip empty nodeId

                    if (NodeDao.selectNodeInfo(nodeId) != null) continue;  // 去重

                    NodeInfo nodeInfo = new NodeInfo();
                    nodeInfo.setNodeId(nodeId);
                    nodeInfo.setBindTime(bindTime);
                    nodeInfo.setIsBind(NodeConst.IS_BIND);
                    nodeInfo.setUserId(userInfo.getId());

                    nodeInfoList.add(nodeInfo);

                    // node rt
                    NodeRt nodeRt = new NodeRt();
                    nodeRt.setNodeId(nodeId);
                    nodeRt.setStatus(NodeConst.STATUS_OFFLINE);
                    nodeRt.setUserId(userInfo.getId());
                    nodeRtList.add(nodeRt);
                }

                if (nodeInfoList.isEmpty()) {
                    r.setCode(ApiCode.INVALID_PARAM);
                    r.setMsg("nodes is empty");
                    return r;
                }

                if (NodeDao.batchInsertNodeInfo(nodeInfoList)) {
                    NodeDao.batchInsertNodeRt(nodeRtList);
                } else {
                    r.setCode(ApiCode.DB_INSERT_ERROR);
                    r.setMsg("batch insert error! check nodes' numbers");
                    return r;
                }
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

        String nodeId = req.get(ApiField.F_nodeId);
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("缺少请求参数");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + "没有找到");
            return r;
        }

        if (nodeInfo.getIsBind() == NodeConst.IS_UNBIND) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + "已经解绑!");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("非法绑定");
            return r;
        }

        NodeInfo unbindNode = new NodeInfo();
        unbindNode.setNodeId(nodeId);
        unbindNode.setIsBind(NodeConst.IS_UNBIND);
        unbindNode.setUnbindTime(System.currentTimeMillis());
        NodeDao.updateNodeInfo(unbindNode);

        // update node_rt
        NodeRt nodeRt = new NodeRt();
        nodeRt.setNodeId(nodeId);
        //nodeRt.setStatus(NodeConst.STATUS_UNKNOWN);
        nodeRt.setUserId(0L);
        NodeDao.updateNodeRt(nodeRt);

        return r;
    }

    @POST
    @Path("node/list")
    public Result<PageRsp<NodeInfoRt>> listNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeListReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeInfoRt>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        NodeRtInfoExample example = new NodeRtInfoExample();
        NodeRtExample countExample = new NodeRtExample(); //TODO opt
        Integer status = req.getStatus();
        if (status == null || status < 0) { //search all
            example.createCriteria().andUserIdEqualTo(userInfo.getId()).andStatusGreaterThanOrEqualTo(0).
                    andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
            countExample.createCriteria().andUserIdEqualTo(userInfo.getId()).andStatusGreaterThanOrEqualTo(0).
                    andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        } else {
            example.createCriteria().andUserIdEqualTo(userInfo.getId()).andStatusEqualTo(status).
                    andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
            countExample.createCriteria().andUserIdEqualTo(userInfo.getId()).andStatusEqualTo(status).
                    andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        }
        example.setOrderByClause("create_time desc limit " + (pageNo - 1) * pageSize + "," + pageSize);
        List<NodeInfoRt> nodeList = NodeDao.selectNodeRtLeftJoinInfo(example);


        long count = NodeDao.countNodeRt(countExample); //TODO cache

        // TODO firmwareUpgrade NodeRsp

        PageRsp<NodeInfoRt> data = new PageRsp<>();
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
        shareNode.setUserId(userInfo.getId());
        shareNode.setIsShare(NodeConst.IS_SHARE);
        shareNode.setShareTime(System.currentTimeMillis());
        Plugin.send(new PluginMsg<NodeInfo>().setType(MsgType.NODE_JOIN_SHARE).setValue(shareNode));

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
        shareNode.setUserId(userInfo.getId());
        shareNode.setNodeId(nodeId);
        shareNode.setIsShare(NodeConst.IS_UNSHARE);
        shareNode.setUnshareTime(System.currentTimeMillis());
        Plugin.send(new PluginMsg<NodeInfo>().setType(MsgType.NODE_QUIT_SHARE).setValue(shareNode));
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

        // user_online
        UserLogin msg = new UserLogin();
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_OFFLINE);
        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

        return r;
    }


    @POST
    @Path("share/stat/sum")
    public Result<ShareSumRsp> sumStatShare(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<ShareSumRsp> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        UserShareSum shareSum = UserDao.selectUserShareSum(userInfo.getId());
        if (shareSum == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("不存在共享收益记录");
            return r;
        }

        ShareSumRsp data = new ShareSumRsp();
        data.setSum(shareSum.getSum());
        data.setBalance(shareSum.getBalance());
        if (shareSum.getLatestTime() != null && (System.currentTimeMillis() - shareSum.getLatestTime()) < 48 * 3600 * 1000)
            data.setYesterday(shareSum.getLatest());
        r.setData(data);
        return r;
    }

    @POST
    @Path("share/stat/day")
    public Result<PageRsp<UserShareDay>> dayStatShare(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, DatePageReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<UserShareDay>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        Long startTime = req.getStartTime();
        Long endTime = req.getEndTime();
        if (startTime == null || endTime == null) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("缺少请求参数");
            return r;
        }

        Long period = endTime - startTime;
        if (period < 0) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("非法请求参数");
            return r;
        }
//        if(period>  90 * 24 * 3600 * 1000){ //TODO 不能超过90
//        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        UserShareDayExample example = new UserShareDayExample();
        example.createCriteria().andUserIdEqualTo(userInfo.getId()).andDateBetween(startTime, endTime).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("date desc limit " + (pageNo - 1) * pageSize + "," + pageSize);
        List<UserShareDay> page = UserDao.selectUserShareDay(example);
        long count = UserDao.countUserShareDay(example);

        PageRsp<UserShareDay> data = new PageRsp<>();
        data.setPage(page);
        data.setCount(count);
        r.setData(data);
        return r;
    }
}